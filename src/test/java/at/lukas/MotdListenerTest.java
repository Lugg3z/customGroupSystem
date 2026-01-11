package at.lukas;

import at.lukas.listener.MotdListener;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotdListenerTest {

    @Mock
    private FileConfiguration config;

    @Mock
    private PaperServerListPingEvent event;

    @Test
    void setsMotdWhenConfigured() {
        // Arrange
        String motd = "&aHello &bWorld";
        when(config.getString("motd")).thenReturn(motd);

        MotdListener listener = new MotdListener(config);

        Component expected = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(motd);

        // Act
        listener.onPing(event);

        // Assert
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(event).motd(captor.capture());

        assertEquals(expected, captor.getValue());
    }

    @Test
    void doesNothingWhenMotdIsNull() {
        // Arrange
        when(config.getString("motd")).thenReturn(null);

        MotdListener listener = new MotdListener(config);

        // Act
        listener.onPing(event);

        // Assert
        verify(event, never()).motd(any());
    }
}
