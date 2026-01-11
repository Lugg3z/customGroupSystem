package at.lukas;

import at.lukas.manager.DatabaseManager;
import at.lukas.misc.PlayerHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerHelperTest {

    @Mock
    Player player;

    @Mock
    DatabaseManager dbManager;

    private final LegacyComponentSerializer serializer =
            LegacyComponentSerializer.legacyAmpersand();

    @Test
    void appliesPrefixFromDatabase() {
        UUID uuid = UUID.randomUUID();

        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Lukas");
        when(dbManager.getPlayerPrefix(uuid)).thenReturn("&a[Admin]");

        PlayerHelper.applyPrefix(player, dbManager);

        Component expected =
                serializer.deserialize("&a[Admin] Lukas");

        verify(player).displayName(expected);
        verify(player).playerListName(expected);
    }

    @Test
    void appliesDefaultPrefixWhenNull() {
        UUID uuid = UUID.randomUUID();

        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Steve");
        when(dbManager.getPlayerPrefix(uuid)).thenReturn(null);

        PlayerHelper.applyPrefix(player, dbManager);

        Component expected =
                serializer.deserialize("&7 Steve");

        verify(player).displayName(expected);
        verify(player).playerListName(expected);
    }

    @Test
    void appliesDefaultPrefixWhenEmpty() {
        UUID uuid = UUID.randomUUID();

        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Alex");
        when(dbManager.getPlayerPrefix(uuid)).thenReturn("");

        PlayerHelper.applyPrefix(player, dbManager);

        Component expected =
                serializer.deserialize("&7 Alex");

        verify(player).displayName(expected);
        verify(player).playerListName(expected);
    }
}
