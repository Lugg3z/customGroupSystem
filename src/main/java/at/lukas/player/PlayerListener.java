package at.lukas.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyPrefix(player);
        setJoinMessage(event, player);
    }

    private void applyPrefix(Player player) {
        //---------------------------------get from database...
        String prefix = "&4[Test]";

        Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(prefix)
                .append(Component.text(' '));

        // getDisplayName und Set sind laut https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Player.html#displayName()
        // deprecated in Paper
        player.displayName(component.append(Component.text(player.getName())));
        player.playerListName(component.append(Component.text(player.getName())));
    }

    private void setJoinMessage(PlayerJoinEvent event, Player player) {
        event.joinMessage(
                player.displayName()
                        .append(Component.text(" joined the server", NamedTextColor.GRAY))
        );
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
