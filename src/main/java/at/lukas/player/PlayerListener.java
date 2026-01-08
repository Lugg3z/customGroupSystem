package at.lukas.player;

import at.lukas.CustomGroupSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static at.lukas.player.PlayerHelper.applyDefaultGroup;
import static at.lukas.player.PlayerHelper.applyPrefix;

public class PlayerListener implements Listener {
    private final DatabaseManager dbManager;

    public PlayerListener(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyDefaultGroup(player, dbManager);
        applyPrefix(player, dbManager);

        event.joinMessage(player.displayName().append(Component.text(" joined the server", NamedTextColor.GRAY)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

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
