package at.lukas.manager;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PermissionManager {

    private final Plugin plugin;
    private final Logger logger;
    private final DatabaseManager dbManager;

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PermissionManager(Plugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.logger = plugin.getLogger();
    }

    public void applyPermissions(Player player) {
        removePermissions(player);

        dbManager.getPlayerPermissions(player.getUniqueId()).thenAccept(permissions -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }

                PermissionAttachment attachment = player.addAttachment(plugin);

                int count = 0;
                for (String permission : permissions) {
                    if (permission.endsWith(".*")) {
                        // Handle wildcard - expand to all matching permissions
                        String prefix = permission.substring(0, permission.length() - 1); // Remove *

                        // Add the wildcard itself
                        attachment.setPermission(permission, true);
                        count++;

                        // Add all registered permissions that start with this prefix
                        for (Permission perm : plugin.getServer().getPluginManager().getPermissions()) {
                            if (perm.getName().startsWith(prefix)) {
                                attachment.setPermission(perm.getName(), true);
                                count++;
                            }
                        }

                        logger.info("Expanded wildcard '" + permission + "' to " + count + " permissions for " + player.getName());
                    } else if (permission.equals("*")) {
                        // Give ALL permissions
                        attachment.setPermission("*", true);
                        for (Permission perm : plugin.getServer().getPluginManager().getPermissions()) {
                            attachment.setPermission(perm.getName(), true);
                        }
                        count = plugin.getServer().getPluginManager().getPermissions().size();
                        logger.info("Gave ALL permissions (*) to " + player.getName());
                    } else {
                        // Normal permission
                        attachment.setPermission(permission, true);
                        count++;
                    }
                }

                attachments.put(player.getUniqueId(), attachment);

                if (count > 0) {
                    logger.info("Applied " + count + " permission(s) to " + player.getName());
                }
            });
        }).exceptionally(e -> {
            logger.warning("Failed to apply permissions for " + player.getName() + ": " + e.getMessage());
            return null;
        });
    }

    public void removePermissions(Player player) {
        removePermissions(player.getUniqueId());
    }

    public void removePermissions(UUID uuid) {
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            attachment.remove();
        }
    }

    public void refreshAllPlayers() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                applyPermissions(player);
            }
            logger.info("Refreshed permissions for all online players");
        });
    }

    public void refreshPlayersInGroup(String groupName) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                String playerGroup = dbManager.getPlayerGroup(player.getUniqueId());
                if (groupName.equalsIgnoreCase(playerGroup)) {
                    applyPermissions(player);
                }
            }
            logger.info("Refreshed permissions for players in group: " + groupName);
        });
    }

    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }
}