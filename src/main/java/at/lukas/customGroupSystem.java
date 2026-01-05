package at.lukas;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class customGroupSystem extends JavaPlugin {
    @Override
    public void onEnable() {
        Logger logger = getLogger();
        logger.info("Plugin wurde gestartet!");
    }
}