package at.lukas.player;

import at.lukas.CustomGroupSystem;
import at.lukas.model.Role;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
    private final CustomGroupSystem plugin;
    private final Map<String, Role> roleCache = new ConcurrentHashMap<>();

    private final Map<UUID, String> playerRoleCache = new ConcurrentHashMap<>();

    public DatabaseManager(CustomGroupSystem plugin) {
        this.plugin = plugin;
    }

    public void loadAllRolesIntoCache() throws SQLException {
        String query = "SELECT id, name, prefix FROM roles";

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            roleCache.clear();

            while (rs.next()) {
                Role role = new Role(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("prefix")
                );
                roleCache.put(role.getName().toLowerCase(), role);
            }

            plugin.getLogger().info("Loaded " + roleCache.size() + " roles into cache");
        }
    }

    public boolean groupExists(String groupName) {
        return roleCache.containsKey(groupName.toLowerCase());
    }

    public List<String> getAllGroups() {
        return new ArrayList<>(roleCache.keySet());
    }

    public String getPrefix(String groupName) {
        Role role = roleCache.get(groupName.toLowerCase());
        if (role == null) return "&7";

        String prefix = role.getPrefix();
        return prefix != null ? prefix : "&7";
    }

    public void createGroup(String name, String prefix) throws SQLException {
        String query = "INSERT INTO roles (name, prefix) VALUES (?, ?)";

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name.toLowerCase());
            stmt.setString(2, prefix);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);

                Role role = new Role(id, name.toLowerCase(), prefix);
                roleCache.put(name.toLowerCase(), role);

                plugin.getLogger().info("Created role: " + name);
            }
        }
    }

    public boolean deleteGroup(String groupName) throws SQLException {
        if (!groupExists(groupName)) {
            return false;
        }

        if (groupName.equalsIgnoreCase("default")) {
            throw new IllegalArgumentException("Cannot delete the default role!");
        }

        List<UUID> affectedPlayers = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerRoleCache.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(groupName)) {
                affectedPlayers.add(entry.getKey());
            }
        }

        String query = "DELETE FROM roles WHERE name = ?";

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, groupName.toLowerCase());
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                roleCache.remove(groupName.toLowerCase());

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

                plugin.getLogger().info("Deleted role: " + groupName + " (" + affectedPlayers.size() + " players reassigned to default)");
                return true;
            }

            return false;
        }
    }

    public void loadPlayerRole(UUID uuid) throws SQLException {
        String query = """
                SELECT r.name 
                FROM player_roles pr
                JOIN roles r ON pr.role_id = r.id
                WHERE pr.uuid = ?
                """;

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String roleName = rs.getString("name");
                playerRoleCache.put(uuid, roleName.toLowerCase());
            } else {
                setUserGroup(uuid, "default");
            }
        }
    }

    public String getPlayerRole(UUID uuid) {
        if (playerRoleCache.containsKey(uuid)) {
            return playerRoleCache.get(uuid);
        }

        try {
            loadPlayerRole(uuid);
            return playerRoleCache.getOrDefault(uuid, "default");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player role: " + e.getMessage());
            return "default";
        }
    }

    public String getPlayerPrefix(UUID uuid) {
        String roleName = getPlayerRole(uuid);
        return getPrefix(roleName);
    }

    public boolean playerHasRole(UUID uuid) {
        return playerRoleCache.containsKey(uuid);
    }

    public void setUserGroup(UUID uuid, String groupName) throws SQLException {
        if (!groupExists(groupName)) {
            throw new IllegalArgumentException("Role does not exist: " + groupName);
        }

        Role role = roleCache.get(groupName.toLowerCase());

        String query = """
                REPLACE INTO player_roles (uuid, role_id, assigned_at)
                VALUES (?, ?, NOW())
                """;

        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            stmt.setInt(2, role.getId());
            stmt.executeUpdate();

            playerRoleCache.put(uuid, groupName.toLowerCase());

            plugin.getLogger().info("Set role for " + uuid + " to " + groupName);
        }
    }

    public void unloadPlayer(UUID uuid) {
        playerRoleCache.remove(uuid);
    }

    public Set<UUID> getCachedPlayers() {
        return new HashSet<>(playerRoleCache.keySet());
    }

    public void clearPlayerCache() {
        playerRoleCache.clear();
    }

    public String getCacheStats() {
        return String.format("Roles cached: %d, Players cached: %d",
                roleCache.size(), playerRoleCache.size());
    }
}