package at.lukas;

import at.lukas.listener.SignListener;
import at.lukas.manager.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignListenerTest {

    @Mock
    DatabaseManager dbManager;

    @Mock
    Player placer;

    @Mock
    Player onlinePlayer;

    @Mock
    OfflinePlayer offlinePlayer;

    @Mock
    SignChangeEvent event;

    private final LegacyComponentSerializer section =
            LegacyComponentSerializer.legacySection();

    private final LegacyComponentSerializer amp =
            LegacyComponentSerializer.legacyAmpersand();

    @Test
    void replacesPlayerNameWithPrefix() {
        UUID uuid = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(placer);
        when(event.line(0)).thenReturn(section.deserialize("%Steve%"));
        when(dbManager.getPlayerPrefix(uuid)).thenReturn("&a[Admin] ");

        when(onlinePlayer.getUniqueId()).thenReturn(uuid);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Steve")).thenReturn(onlinePlayer);

            SignListener listener = new SignListener(dbManager);
            listener.onSignChange(event);

            verify(event).line(
                    eq(0),
                    eq(amp.deserialize("&a[Admin] Steve"))
            );
        }
    }

    @Test
    void replacesGroupPlaceholderOnly() {
        UUID uuid = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(placer);
        when(event.line(0)).thenReturn(section.deserialize("%Steve%group%"));
        when(dbManager.getPlayerPrefix(uuid)).thenReturn("&b[VIP]");

        when(onlinePlayer.getUniqueId()).thenReturn(uuid);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Steve")).thenReturn(onlinePlayer);

            SignListener listener = new SignListener(dbManager);
            listener.onSignChange(event);

            verify(event).line(
                    eq(0),
                    eq(amp.deserialize("&b[VIP]"))
            );
        }
    }

    @Test
    void unknownPlayerReplacedWithUnknown() {
        when(event.getPlayer()).thenReturn(placer);
        when(event.line(0)).thenReturn(section.deserialize("%Ghost%"));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Ghost")).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayer("Ghost"))
                    .thenReturn(offlinePlayer);

            when(offlinePlayer.hasPlayedBefore()).thenReturn(false);

            SignListener listener = new SignListener(dbManager);
            listener.onSignChange(event);

            verify(event).line(
                    eq(0),
                    eq(amp.deserialize("&c[Unknown]"))
            );
        }
    }

    @Test
    void offlinePlayerWithHistoryIsResolved() {
        UUID uuid = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(placer);
        when(event.line(0)).thenReturn(section.deserialize("%Alex%"));

        when(offlinePlayer.hasPlayedBefore()).thenReturn(true);
        when(offlinePlayer.getUniqueId()).thenReturn(uuid);
        when(dbManager.getPlayerPrefix(uuid)).thenReturn("&6[Legend] ");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Alex")).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayer("Alex"))
                    .thenReturn(offlinePlayer);

            SignListener listener = new SignListener(dbManager);
            listener.onSignChange(event);

            verify(event).line(
                    eq(0),
                    eq(amp.deserialize("&6[Legend] Alex"))
            );
        }
    }

    @Test
    void lineWithoutPlaceholderIsIgnored() {
        Component original = section.deserialize("Hello World");

        when(event.getPlayer()).thenReturn(placer);
        when(event.line(0)).thenReturn(original);

        SignListener listener = new SignListener(dbManager);
        listener.onSignChange(event);

        verify(event, never()).line(eq(0), any());
    }

    @Test
    void nullLineIsIgnored() {
        when(event.getPlayer()).thenReturn(placer);
        when(event.line(0)).thenReturn(null);

        SignListener listener = new SignListener(dbManager);
        listener.onSignChange(event);

        verify(event, never()).line(eq(0), any());
    }
}
