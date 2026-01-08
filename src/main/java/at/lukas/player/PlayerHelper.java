package at.lukas.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class PlayerHelper {
    public static void applyPrefix(Player player, DatabaseManager dbManager) {
        //---------------------------------get from database...
        String prefix = dbManager.getPlayerPrefix(player.getUniqueId());

        Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(prefix)
                .append(Component.text(' '));

        // getDisplayName und Set sind laut https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Player.html#displayName()
        // deprecated in Paper
        player.displayName(component.append(Component.text(player.getName())));
        player.playerListName(component.append(Component.text(player.getName())));
    }

    public static void applyDefaultGroup(Player player, DatabaseManager dbManager) {
        if(!dbManager.playerHasRole(player.getUniqueId())) {
            dbManager.setUserGroup(player.getUniqueId(), "Default");
        }
    }
}
