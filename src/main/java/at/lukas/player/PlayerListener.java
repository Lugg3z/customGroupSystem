package at.lukas.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
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

        try {
            dbManager.loadPlayerGroup(player.getUniqueId());
            applyPrefix(player, dbManager);
        } catch (SQLException e) {
            player.sendMessage("§cError loading your group data!");
            e.printStackTrace();
        }

        event.joinMessage(player.displayName().append(Component.text(" joined the server", NamedTextColor.GRAY)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        dbManager.unloadPlayer(player.getUniqueId());

        event.quitMessage(player.displayName().append(Component.text(" left the server")));
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