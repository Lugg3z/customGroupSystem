package at.lukas;

import at.lukas.commands.GroupSystemCommand;
import at.lukas.commands.GroupSystemTabCompleter;
import at.lukas.misc.ExpiryCheckTask;
import at.lukas.misc.MotdListener;
import at.lukas.misc.SignListener;
import at.lukas.player.DatabaseManager;
import at.lukas.player.PermissionManager;
import at.lukas.player.PlayerListener;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;

public class CustomGroupSystem extends JavaPlugin {
    private final Logger logger = getLogger();
    private HikariDataSource dataSource;
    private DatabaseManager dbManager;
    private PermissionManager permissionManager;

    private BukkitTask expiryCheckTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!initializeDatabase()) {
            logger.severe("Failed to initialize database - disabling plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeManagers();

        registerCommands(permissionManager);

        registerEvents();

        startExpiryCheckTask();

        logger.info("CustomGroupSystem enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (expiryCheckTask != null) {
            expiryCheckTask.cancel();
            logger.info("Expiry check task cancelled.");
        }

        if (dbManager != null) {
            dbManager.shutdown();
        }

        closeDatabase();
        logger.info("CustomGroupSystem disabled.");
    }

    private boolean initializeDatabase() {
        try {
            connectToDatabase();
            createTablesOnFirstBoot();
            return true;
        } catch (SQLException e) {
            logger.severe("Database initialization failed: " + e.getMessage());
            return false;
        }
    }

    private void connectToDatabase() throws SQLException {
        logger.info("Connecting to database...");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:3307/minecraft");
        config.setUsername("mcuser");
        config.setPassword("mcuser");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);

        dataSource = new HikariDataSource(config);

        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                logger.info("Database connected successfully!");
            }
        }
    }

    private void createTablesOnFirstBoot() throws SQLException {
        String createGroupsTable = """
                CREATE TABLE IF NOT EXISTS group_data (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(36) NOT NULL UNIQUE,
                    prefix VARCHAR(64)
                )
                """;

        String createPlayerGroupsTable = """
                CREATE TABLE IF NOT EXISTS player_groups (
                    uuid CHAR(36) NOT NULL PRIMARY KEY,
                    group_id INT NOT NULL,
                    expiry DATETIME NULL,
                    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_player_groups_group
                        FOREIGN KEY (group_id)
                        REFERENCES group_data(id)
                        ON DELETE CASCADE,
                    INDEX idx_player_groups_group (group_id),
                    INDEX idx_player_groups_expiry (expiry)
                )
                """;

        String insertDefaultGroups = """
                INSERT IGNORE INTO group_data (name, prefix) VALUES
                ('default', '&7[Member]'),
                ('vip', '&6[VIP]'),
                ('admin', '&c[Admin]')
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createGroupsTable);
            statement.execute(createPlayerGroupsTable);
            statement.execute(insertDefaultGroups);

            logger.info("Database tables initialized successfully.");
        }
    }

    private void closeDatabase() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection closed.");
        }
    }

    private void initializeManagers() {
        dbManager = new DatabaseManager(this);
        permissionManager = new PermissionManager();

        dbManager.loadAllGroupsIntoCache().thenRun(() -> {
            logger.info("Loaded " + dbManager.getAllGroups().size() + " groups into cache.");
        }).exceptionally(e -> {
            logger.warning("Failed to load groups into cache: " + e.getMessage());
            return null;
        });
    }

    private void registerCommands(PermissionManager permissionManager) {
        GroupSystemCommand command = new GroupSystemCommand(permissionManager, dbManager, this);
        GroupSystemTabCompleter tabCompleter = new GroupSystemTabCompleter(dbManager);

        Objects.requireNonNull(getCommand("gs")).setExecutor(command);
        Objects.requireNonNull(getCommand("gs")).setTabCompleter(tabCompleter);

        logger.info("Commands registered.");
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new PlayerListener(dbManager, logger), this);
        pm.registerEvents(new MotdListener(getConfig()), this);
        pm.registerEvents(new SignListener(dbManager), this);
        logger.info("Event listeners registered.");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    private void startExpiryCheckTask() {
        expiryCheckTask = new ExpiryCheckTask(this, dbManager)
                .runTaskTimerAsynchronously(this, 200L, 200L);

        logger.info("Expiry check task started (runs every 10 seconds)");
    }
}