package at.lukas.misc;

import at.lukas.CustomGroupSystem;
import at.lukas.player.DatabaseManager;
import org.bukkit.scheduler.BukkitRunnable;

public class ExpiryCheckTask extends BukkitRunnable {

    private final CustomGroupSystem plugin;
    private final DatabaseManager dbManager;

    public ExpiryCheckTask(CustomGroupSystem plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        dbManager.removeExpiredGroups().thenAccept(count -> {
            if (count > 0) {
                plugin.getLogger().info("Removed " + count + " expired group(s)");
            }
        });

    }
}