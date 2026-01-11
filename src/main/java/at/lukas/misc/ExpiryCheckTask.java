package at.lukas.misc;

import at.lukas.CustomGroupSystem;
import at.lukas.manager.DatabaseManager;
import at.lukas.manager.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ExpiryCheckTask extends BukkitRunnable {

    private final CustomGroupSystem plugin;
    private final DatabaseManager dbManager;
    private final PermissionManager permissionManager;

    public ExpiryCheckTask(CustomGroupSystem plugin, DatabaseManager dbManager, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.permissionManager = permissionManager;
    }

    @Override
    public void run() {
        dbManager.removeExpiredGroups().thenAccept(affectedPlayers -> {
            if (affectedPlayers > 0) {
                plugin.getLogger().info("Removed " + affectedPlayers + " expired group(s)");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String playerGroup = dbManager.getPlayerGroup(player.getUniqueId());
                        if ("default".equalsIgnoreCase(playerGroup)) {
                            permissionManager.applyPermissions(player);
                            PlayerHelper.applyPrefix(player, dbManager);
                        }
                    }
                });
            }
        }).exceptionally(e -> {
            plugin.getLogger().warning("Error checking expired groups: " + e.getMessage());
            return null;
        });
    }
}