package at.lukas.player;

import at.lukas.CustomGroupSystem;
import at.lukas.model.Group;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
    private final CustomGroupSystem plugin;
    private final Map<String, Group> groupCache = new ConcurrentHashMap<>();

    private final Map<UUID, String> playerGroupCache = new ConcurrentHashMap<>();

    public DatabaseManager(CustomGroupSystem plugin) {
        this.plugin = plugin;
    }

    public void loadAllGroupsIntoCache() throws SQLException {
        String query = "SELECT id, name, prefix FROM group_data";

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            groupCache.clear();

            while (rs.next()) {
                Group group = new Group(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("prefix")
                );
                groupCache.put(group.getName().toLowerCase(), group);
            }

            plugin.getLogger().info("Loaded " + groupCache.size() + " groups into cache");
        }
    }

    public boolean groupExists(String groupName) {
        return groupCache.containsKey(groupName.toLowerCase());
    }

    public List<String> getAllGroups() {
        return new ArrayList<>(groupCache.keySet());
    }

    public String getPrefix(String groupName) {
        Group group = groupCache.get(groupName.toLowerCase());
        if (group == null) return "&7";

        String prefix = group.getPrefix();
        return prefix != null ? prefix : "&7";
    }

    public void createGroup(String name, String prefix) throws SQLException {
        String query = "INSERT INTO group_data (name, prefix) VALUES (?, ?)";

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name.toLowerCase());
            stmt.setString(2, prefix);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);

                Group group = new Group(id, name.toLowerCase(), prefix);
                groupCache.put(name.toLowerCase(), group);

                plugin.getLogger().info("Created group: " + name);
            }
        }
    }

    public boolean deleteGroup(String groupName) throws SQLException {
        if (!groupExists(groupName)) {
            return false;
        }

        if (groupName.equalsIgnoreCase("default")) {
            throw new IllegalArgumentException("Cannot delete the default group!");
        }

        List<UUID> affectedPlayers = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerGroupCache.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(groupName)) {
                affectedPlayers.add(entry.getKey());
            }
        }

        String query = "DELETE FROM group_data WHERE name = ?";

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, groupName.toLowerCase());
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                groupCache.remove(groupName.toLowerCase());

                for (UUID uuid : affectedPlayers) {
                    try {
                        setUserGroup(uuid, "default");

                        Player player = plugin.getServer().getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            PlayerHelper.applyPrefix(player, this);
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Failed to reassign player " + uuid + " to default: " + e.getMessage());
                    }
                }

                plugin.getLogger().info("Deleted group: " + groupName + " (" + affectedPlayers.size() + " players reassigned to default)");
                return true;
            }

            return false;
        }
    }

    public void loadPlayerGroup(UUID uuid) throws SQLException {
        String query = """
                SELECT g.name
                FROM player_groups pg
                JOIN group_data g ON pg.group_id = g.id
                WHERE pg.uuid = ?
                """;

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String groupName = rs.getString("name");
                playerGroupCache.put(uuid, groupName.toLowerCase());
            } else {
                setUserGroup(uuid, "default");
            }
        }
    }

    public String getPlayerGroup(UUID uuid) {
        if (playerGroupCache.containsKey(uuid)) {
            return playerGroupCache.get(uuid);
        }

        try {
            loadPlayerGroup(uuid);
            return playerGroupCache.getOrDefault(uuid, "default");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player group: " + e.getMessage());
            return "default";
        }
    }

    public String getPlayerPrefix(UUID uuid) {
        String groupName = getPlayerGroup(uuid);
        return getPrefix(groupName);
    }

    public boolean playerHasGroup(UUID uuid) {
        return playerGroupCache.containsKey(uuid);
    }

    public void setUserGroup(UUID uuid, String groupName) throws SQLException {
        setUserGroup(uuid, groupName, null);
    }

    public void setUserGroup(UUID uuid, String groupName, Long expiryMillis) throws SQLException {
        if (!groupExists(groupName)) {
            throw new IllegalArgumentException("Group does not exist: " + groupName);
        }

        Group group = groupCache.get(groupName.toLowerCase());

        String query = getQuery(expiryMillis);

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            stmt.setInt(2, group.getId());

            if (expiryMillis != null) {
                stmt.setLong(3, expiryMillis / 1000); // Convert to seconds for MySQL
            }

            stmt.executeUpdate();

            playerGroupCache.put(uuid, groupName.toLowerCase());

            plugin.getLogger().info("Set group for " + uuid + " to " + groupName +
                    (expiryMillis == null ? " (permanent)" : " (expires: " + new java.util.Date(expiryMillis) + ")"));
        }
    }

    private static @NotNull String getQuery(Long expiryMillis) {
        String query;
        if (expiryMillis == null) {
            // Permanent
            query = """
                    REPLACE INTO player_groups (uuid, group_id, expiry, assigned_at)
                    VALUES (?, ?, NULL, NOW())
                    """;
        } else {
            // Temporary with expiry
            query = """
                    REPLACE INTO player_groups (uuid, group_id, expiry, assigned_at)
                    VALUES (?, ?, FROM_UNIXTIME(?), NOW())
                    """;
        }
        return query;
    }

    public void unloadPlayer(UUID uuid) {
        playerGroupCache.remove(uuid);
    }

    public Set<UUID> getCachedPlayers() {
        return new HashSet<>(playerGroupCache.keySet());
    }

    public void clearPlayerCache() {
        playerGroupCache.clear();
    }

    public String getCacheStats() {
        return String.format("Groups cached: %d, Players cached: %d",
                groupCache.size(), playerGroupCache.size());
    }

    public Long getPlayerGroupExpiry(UUID uuid) throws SQLException {
        String query = """
                SELECT UNIX_TIMESTAMP(expiry) * 1000 as expiry_millis
                FROM player_groups
                WHERE uuid = ?
                """;

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                long expiryMillis = rs.getLong("expiry_millis");
                return rs.wasNull() ? null : expiryMillis;
            }

            return null;
        }
    }

    public int removeExpiredGroups() throws SQLException {
        String query = """
                SELECT pg.uuid, g.name
                FROM player_groups pg
                JOIN group_data g ON pg.group_id = g.id
                WHERE pg.expiry IS NOT NULL AND pg.expiry <= NOW()
                """;

        List<UUID> expiredPlayers = new ArrayList<>();

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String groupName = rs.getString("name");
                expiredPlayers.add(uuid);

                plugin.getLogger().info("Group expired for " + uuid + " (was: " + groupName + ")");
            }
        }

        // Silently reset to default
        for (UUID uuid : expiredPlayers) {
            setUserGroup(uuid, "default", null);

            // Refresh prefix if online (no messages, just visual update)
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PlayerHelper.applyPrefix(player, this);
            }
        }

        return expiredPlayers.size();
    }
}