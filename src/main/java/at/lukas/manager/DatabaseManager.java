package at.lukas.manager;

import at.lukas.CustomGroupSystem;
import at.lukas.misc.PlayerHelper;
import at.lukas.model.Group;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {
    private final CustomGroupSystem plugin;
    private final Map<String, Group> groupCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerGroupCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    public DatabaseManager(CustomGroupSystem plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> loadAllGroupsIntoCache() {
        return CompletableFuture.runAsync(() -> {
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
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load groups: " + e.getMessage());
            }
        }, executorService);
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

    public CompletableFuture<Void> createGroup(String name, String prefix) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO group_data (name, prefix) VALUES (?, ?)";

            try (Connection conn = plugin.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, name.toLowerCase());
                stmt.setString(2, prefix);
                stmt.executeUpdate();

                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int groupId = generatedKeys.getInt(1);

                    Group group = new Group(groupId, name.toLowerCase(), prefix);
                    groupCache.put(name.toLowerCase(), group);

                    plugin.getLogger().info("Created group: " + name);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create group: " + e.getMessage());
            }
        }, executorService);
    }

    public CompletableFuture<Boolean> deleteGroup(String groupName) {
        if (!groupExists(groupName)) {
            return CompletableFuture.completedFuture(false);
        }

        if (groupName.equalsIgnoreCase("default")) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Cannot delete the default group!"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            List<UUID> affectedPlayers = findPlayersInGroup(groupName);

            String deleteQuery = "DELETE FROM group_data WHERE name = ?";

            try (Connection conn = plugin.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {

                stmt.setString(1, groupName.toLowerCase());
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    groupCache.remove(groupName.toLowerCase());
                    reassignPlayersToDefault(affectedPlayers);

                    plugin.getLogger().info("Deleted group: " + groupName + " (" + affectedPlayers.size() + " players reassigned to default)");
                    return true;
                }

                return false;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete group: " + e.getMessage());
                return false;
            }
        }, executorService);
    }

    public CompletableFuture<Void> loadPlayerGroup(UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            String query = """
                    SELECT g.name
                    FROM player_groups pg
                    JOIN group_data g ON pg.group_id = g.id
                    WHERE pg.uuid = ?
                    """;

            try (Connection conn = plugin.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String groupName = rs.getString("name");
                    playerGroupCache.put(playerUuid, groupName.toLowerCase());
                } else {
                    setUserGroupSync(playerUuid, "default");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player group: " + e.getMessage());
            }
        }, executorService);
    }

    public String getPlayerGroup(UUID playerUuid) {
        return playerGroupCache.getOrDefault(playerUuid, "default");
    }

    public String getPlayerPrefix(UUID playerUuid) {
        String groupName = getPlayerGroup(playerUuid);
        return getPrefix(groupName);
    }

    public boolean playerHasGroup(UUID playerUuid) {
        return playerGroupCache.containsKey(playerUuid);
    }

    public CompletableFuture<Void> setUserGroup(UUID playerUuid, String groupName) {
        return setUserGroup(playerUuid, groupName, null);
    }

    public CompletableFuture<Void> setUserGroup(UUID playerUuid, String groupName, Long expiryMillis) {
        if (!groupExists(groupName)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Group does not exist: " + groupName));
            return future;
        }

        return CompletableFuture.runAsync(() -> {
            try {
                setUserGroupSync(playerUuid, groupName, expiryMillis);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set user group: " + e.getMessage());
            }
        }, executorService);
    }

    private void setUserGroupSync(UUID playerUuid, String groupName) throws SQLException {
        setUserGroupSync(playerUuid, groupName, null);
    }

    private void setUserGroupSync(UUID playerUuid, String groupName, Long expiryMillis) throws SQLException {
        Group group = groupCache.get(groupName.toLowerCase());
        String upsertQuery = buildUserGroupUpsertQuery(expiryMillis);

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(upsertQuery)) {

            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, group.getId());

            if (expiryMillis != null) {
                stmt.setLong(3, expiryMillis / 1000);
            }

            stmt.executeUpdate();
            playerGroupCache.put(playerUuid, groupName.toLowerCase());

            plugin.getLogger().info("Set group for " + playerUuid + " to " + groupName +
                    (expiryMillis == null ? " (permanent)" : " (expires: " + new java.util.Date(expiryMillis) + ")"));
        }
    }

    public CompletableFuture<Long> getPlayerGroupExpiry(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String query = """
                    SELECT UNIX_TIMESTAMP(expiry) * 1000 as expiry_millis
                    FROM player_groups
                    WHERE uuid = ?
                    """;

            try (Connection conn = plugin.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    long expiryMillis = rs.getLong("expiry_millis");
                    return rs.wasNull() ? null : expiryMillis;
                }

                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get expiry: " + e.getMessage());
                return null;
            }
        }, executorService);
    }

    public CompletableFuture<Integer> removeExpiredGroups() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> expiredPlayerUuids = findExpiredPlayers();

            for (UUID playerUuid : expiredPlayerUuids) {
                try {
                    setUserGroupSync(playerUuid, "default", null);
                    applyPrefixToOnlinePlayer(playerUuid);
                } catch (SQLException e) {
                    plugin.getLogger().warning("Failed to reset expired group: " + e.getMessage());
                }
            }

            return expiredPlayerUuids.size();
        }, executorService);
    }

    public void unloadPlayer(UUID playerUuid) {
        playerGroupCache.remove(playerUuid);
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

    public void shutdown() {
        executorService.shutdown();
    }

    private @NotNull String buildUserGroupUpsertQuery(Long expiryMillis) {
        if (expiryMillis == null) {
            return """
                    REPLACE INTO player_groups (uuid, group_id, expiry, assigned_at)
                    VALUES (?, ?, NULL, NOW())
                    """;
        } else {
            return """
                    REPLACE INTO player_groups (uuid, group_id, expiry, assigned_at)
                    VALUES (?, ?, FROM_UNIXTIME(?), NOW())
                    """;
        }
    }

    private List<UUID> findPlayersInGroup(String groupName) {
        List<UUID> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerGroupCache.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(groupName)) {
                players.add(entry.getKey());
            }
        }
        return players;
    }

    private void reassignPlayersToDefault(List<UUID> playerUuids) {
        for (UUID playerUuid : playerUuids) {
            try {
                setUserGroupSync(playerUuid, "default");
                applyPrefixToOnlinePlayer(playerUuid);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to reassign player " + playerUuid + " to default: " + e.getMessage());
            }
        }
    }

    private void applyPrefixToOnlinePlayer(UUID playerUuid) {
        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                PlayerHelper.applyPrefix(player, this));
        }
    }

    private List<UUID> findExpiredPlayers() {
        String query = """
                SELECT pg.uuid, g.name
                FROM player_groups pg
                JOIN group_data g ON pg.group_id = g.id
                WHERE pg.expiry IS NOT NULL AND pg.expiry <= NOW()
                """;

        List<UUID> expiredPlayerUuids = new ArrayList<>();

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("uuid"));
                String groupName = rs.getString("name");
                expiredPlayerUuids.add(playerUuid);

                plugin.getLogger().info("Group expired for " + playerUuid + " (was: " + groupName + ")");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check expired groups: " + e.getMessage());
        }

        return expiredPlayerUuids;
    }
}

