package at.lukas.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.awt.*;
import java.util.logging.Logger;

import static at.lukas.player.PlayerHelper.applyPrefix;

public class PlayerListener implements Listener {
    private final DatabaseManager dbManager;
    private final Logger logger;

    public PlayerListener(DatabaseManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Suppress the default join message since we'll send a custom one
        event.joinMessage(null);

        dbManager.loadPlayerGroup(player.getUniqueId()).thenRun(() -> {
            applyPrefix(player, dbManager);

            if (player.isOnline()) {
                Component joinMsg = player.displayName()
                        .append(Component.text(" joined the server", NamedTextColor.YELLOW));
                Bukkit.broadcast(joinMsg);
            }
        }).exceptionally(e -> {
            if (player.isOnline()) {
                player.sendMessage("§cError loading your group data!");

                Component joinMsg = Component.text(player.getName() + " joined the server", NamedTextColor.YELLOW);
                Bukkit.broadcast(joinMsg);
            }
            logger.severe("Error loading group data for player " + player.getName() + ": " + e.getMessage());
            return null;
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        dbManager.unloadPlayer(player.getUniqueId());

        event.quitMessage(player.displayName().append(Component.text(" left the server", NamedTextColor.YELLOW)));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        Component original = event.deathMessage();
        if (original == null) return;

        event.deathMessage(
                Component.text()
                        .append(player.displayName())
                        .append(original.replaceText(builder ->
                                builder.match(player.getName()).replacement(Component.empty())
                        ))
                        .build()
        );
    }
}