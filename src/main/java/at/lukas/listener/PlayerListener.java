package at.lukas.listener;

import at.lukas.manager.DatabaseManager;
import at.lukas.manager.PermissionManager;
import at.lukas.misc.PlayerHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;

public class PlayerListener implements Listener {

    private final DatabaseManager dbManager;
    private final PermissionManager permissionManager;
    private final Logger logger;

    public PlayerListener(DatabaseManager dbManager, PermissionManager permissionManager, Logger logger) {
        this.dbManager = dbManager;
        this.permissionManager = permissionManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.joinMessage(null);

        dbManager.loadPlayerGroup(player.getUniqueId()).thenRun(() -> {
            Bukkit.getScheduler().runTask(dbManager.getPlugin(), () -> {
                if (player.isOnline()) {
                    PlayerHelper.applyPrefix(player, dbManager);
                    permissionManager.applyPermissions(player);

                    String prefix = dbManager.getPlayerPrefix(player.getUniqueId());
                    broadcastPlayerJoinMessage(player);
                }
            });
        }).exceptionally(e -> {
            logger.severe("Error loading player data for " + player.getName() + ": " + e.getMessage());

            dbManager.setUserGroup(player.getUniqueId(), "default", null).thenRun(() -> {
                Bukkit.getScheduler().runTask(dbManager.getPlugin(), () -> {
                    if (player.isOnline()) {
                        PlayerHelper.applyPrefix(player, dbManager);
                        permissionManager.applyPermissions(player);

                        String prefix = dbManager.getPlayerPrefix(player.getUniqueId());
                        broadcastPlayerJoinMessage(player);
                    }
                });
            });

            return null;
        });
    }

    private void broadcastPlayerJoinMessage(Player player) {
        Component joinMessage = player.displayName()
                .append(Component.text(" joined the server", NamedTextColor.YELLOW));
        Bukkit.broadcast(joinMessage);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        String prefix = dbManager.getPlayerPrefix(player.getUniqueId());
        event.quitMessage(player.displayName().append(Component.text(" left the server", NamedTextColor.YELLOW)));

        dbManager.unloadPlayer(player.getUniqueId());
        permissionManager.removePermissions(player);
    }
}