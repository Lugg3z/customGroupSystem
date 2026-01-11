package at.lukas.misc;

import at.lukas.CustomGroupSystem;
import at.lukas.player.DatabaseManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;

public class ExpiryCheckTask extends BukkitRunnable {

    private final CustomGroupSystem plugin;
    private final DatabaseManager dbManager;

    public ExpiryCheckTask(CustomGroupSystem plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        try {
            int removed = dbManager.removeExpiredGroups();

            if (removed > 0) {
                plugin.getLogger().info("Removed " + removed + " expired group(s)");
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking expired groups: " + e.getMessage());
        }
    }
}