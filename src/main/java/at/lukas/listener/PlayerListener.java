package at.lukas.listener;

import at.lukas.manager.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;

import static at.lukas.misc.PlayerHelper.applyPrefix;

public class PlayerListener implements Listener {
    private final DatabaseManager dbManager;
    private final Logger logger;

    public PlayerListener(DatabaseManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null);

        dbManager.loadPlayerGroup(player.getUniqueId()).thenRun(() -> {
            applyPrefix(player, dbManager);

            if (player.isOnline()) {
                broadcastPlayerJoinMessage(player);
            }
        }).exceptionally(e -> {
            handleGroupLoadError(player, e);
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dbManager.unloadPlayer(player.getUniqueId());

        event.quitMessage(player.displayName().append(Component.text(" left the server", NamedTextColor.YELLOW)));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Component deathMessage = event.deathMessage();

        if (deathMessage == null) return;

        event.deathMessage(
                Component.text()
                        .append(player.displayName())
                        .append(deathMessage.replaceText(builder ->
                                builder.match(player.getName()).replacement(Component.empty())
                        ))
                        .build()
        );
    }

    private void broadcastPlayerJoinMessage(Player player) {
        Component joinMessage = player.displayName()
                .append(Component.text(" joined the server", NamedTextColor.YELLOW));
        Bukkit.broadcast(joinMessage);
    }

    private void handleGroupLoadError(Player player, Throwable error) {
        if (player.isOnline()) {
            player.sendMessage("§cError loading your group data!");

            Component joinMessage = Component.text(player.getName() + " joined the server", NamedTextColor.YELLOW);
            Bukkit.broadcast(joinMessage);
        }

        logger.severe("Error loading group data for player " + player.getName() + ": " + error.getMessage());
    }
}