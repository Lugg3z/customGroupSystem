package at.lukas.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class PlayerHelper {
    public static void applyPrefix(Player player, DatabaseManager dbManager) {
        String prefix = dbManager.getPlayerPrefix(player.getUniqueId());

        // Safety check
        if (prefix == null || prefix.isEmpty()) {
            prefix = "&7";
        }

        Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(prefix)
                .append(Component.text(' '));

        player.displayName(component.append(Component.text(player.getName())));
        player.playerListName(component.append(Component.text(player.getName())));
    }
}
