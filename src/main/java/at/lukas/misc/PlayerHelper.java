package at.lukas.misc;

import at.lukas.manager.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class PlayerHelper {
    public static void applyPrefix(Player player, DatabaseManager dbManager) {
        String prefix = dbManager.getPlayerPrefix(player.getUniqueId());

        if (prefix == null || prefix.isEmpty()) {
            prefix = "&7";
        }

        Component displayName = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(prefix + " " + player.getName());

        player.displayName(displayName);
        player.playerListName(displayName);
    }
}
