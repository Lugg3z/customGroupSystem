package at.lukas;

import at.lukas.listener.PlayerListener;
import at.lukas.manager.DatabaseManager;
import at.lukas.misc.PlayerHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerListenerTest {

    @Mock
    DatabaseManager dbManager;

    @Mock
    Logger logger;

    @Mock
    Player player;

    @Mock
    PlayerJoinEvent joinEvent;

    @Mock
    PlayerQuitEvent quitEvent;

    @Mock
    PlayerDeathEvent deathEvent;

    @Test
    void joinLoadsGroupAppliesPrefixAndBroadcasts() {
        UUID uuid = UUID.randomUUID();

        when(joinEvent.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);
        when(player.displayName()).thenReturn(Component.text("Lukas"));
        when(dbManager.loadPlayerGroup(uuid))
                .thenReturn(CompletableFuture.completedFuture(null));

        PlayerListener listener = new PlayerListener(dbManager, logger);

        try (
                MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class);
                MockedStatic<PlayerHelper> helperMock = mockStatic(PlayerHelper.class)
        ) {
            listener.onPlayerJoin(joinEvent);

            verify(joinEvent).joinMessage(null);
            helperMock.verify(() -> PlayerHelper.applyPrefix(player, dbManager));

            bukkitMock.verify(() ->
                    Bukkit.broadcast(
                            Component.text("Lukas")
                                    .append(Component.text(" joined the server", NamedTextColor.YELLOW))
                    )
            );
        }
    }

    @Test
    void joinHandlesDatabaseError() {
        UUID uuid = UUID.randomUUID();
        RuntimeException error = new RuntimeException("DB down");

        when(joinEvent.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);
        when(player.getName()).thenReturn("Lukas");

        when(dbManager.loadPlayerGroup(uuid))
                .thenReturn(CompletableFuture.failedFuture(error));

        PlayerListener listener = new PlayerListener(dbManager, logger);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            listener.onPlayerJoin(joinEvent);

            verify(player).sendMessage("§cError loading your group data!");
            verify(logger).severe(contains("DB down"));

            bukkitMock.verify(() ->
                    Bukkit.broadcast(
                            Component.text("Lukas joined the server", NamedTextColor.YELLOW)
                    )
            );
        }
    }

    @Test
    void quitUnloadsPlayerAndSetsQuitMessage() {
        UUID uuid = UUID.randomUUID();

        when(quitEvent.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.displayName()).thenReturn(Component.text("Lukas"));

        PlayerListener listener = new PlayerListener(dbManager, logger);

        listener.onPlayerQuit(quitEvent);

        verify(dbManager).unloadPlayer(uuid);
        verify(quitEvent).quitMessage(
                Component.text("Lukas")
                        .append(Component.text(" left the server", NamedTextColor.YELLOW))
        );
    }

    @Test
    void deathMessageIsRewritten() {
        when(deathEvent.getPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Lukas");
        when(player.displayName()).thenReturn(Component.text("§aLukas"));

        Component original = Component.text("Lukas was slain by Zombie");
        when(deathEvent.deathMessage()).thenReturn(original);

        PlayerListener listener = new PlayerListener(dbManager, logger);
        listener.onPlayerDeath(deathEvent);

        verify(deathEvent).deathMessage(any(Component.class));
    }

    @Test
    void nullDeathMessageDoesNothing() {
        when(deathEvent.deathMessage()).thenReturn(null);

        PlayerListener listener = new PlayerListener(dbManager, logger);
        listener.onPlayerDeath(deathEvent);

        verify(deathEvent, never()).deathMessage(any());
    }
}
