package at.lukas;

import at.lukas.manager.DatabaseManager;
import at.lukas.misc.ExpiryCheckTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiryCheckTaskTest {

    @Mock
    CustomGroupSystem plugin;

    @Mock
    DatabaseManager dbManager;

    @Mock
    Logger logger;

    @Test
    void logsWhenExpiredGroupsAreRemoved() {
        // Arrange
        when(plugin.getLogger()).thenReturn(logger);
        when(dbManager.removeExpiredGroups())
                .thenReturn(CompletableFuture.completedFuture(3));

        ExpiryCheckTask task = new ExpiryCheckTask(plugin, dbManager);

        // Act
        task.run();

        // Assert
        verify(dbManager).removeExpiredGroups();
        verify(logger).info("Removed 3 expired group(s)");
    }

    @Test
    void doesNotLogWhenNoGroupsRemoved() {
        when(dbManager.removeExpiredGroups())
                .thenReturn(CompletableFuture.completedFuture(0));

        ExpiryCheckTask task = new ExpiryCheckTask(plugin, dbManager);
        task.run();

        verify(dbManager).removeExpiredGroups();
        verify(plugin, never()).getLogger();
    }
}
