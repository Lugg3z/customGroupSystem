package at.lukas;

import at.lukas.commands.GroupSystemCommand;
import at.lukas.commands.GroupSystemTabCompleter;
import at.lukas.listener.MotdListener;
import at.lukas.listener.PlayerListener;
import at.lukas.listener.SignListener;
import at.lukas.manager.DatabaseManager;
import at.lukas.manager.PermissionManager;
import at.lukas.misc.ExpiryCheckTask;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
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
        registerCommands();
        registerEventListeners();
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

    protected void connectToDatabase() throws SQLException {
        logger.info("Connecting to database...");

        FileConfiguration config = getConfig();

        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "minecraft");
        String username = config.getString("database.username", "mcuser");
        String password = config.getString("database.password", "mcuser");

        int maxPoolSize = config.getInt("database.pool.maximum-pool-size", 10);
        int minIdle = config.getInt("database.pool.minimum-idle", 2);
        int connectionTimeout = config.getInt("database.pool.connection-timeout", 30000);

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);

        logger.info("Connecting to: " + jdbcUrl);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setConnectionTimeout(connectionTimeout);

        dataSource = new HikariDataSource(hikariConfig);

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

        // NEW: Group permissions table
        String createGroupPermissionsTable = """
                CREATE TABLE IF NOT EXISTS group_permissions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    group_id INT NOT NULL,
                    permission VARCHAR(255) NOT NULL,
                    CONSTRAINT fk_group_permissions_group
                        FOREIGN KEY (group_id)
                        REFERENCES group_data(id)
                        ON DELETE CASCADE,
                    UNIQUE KEY unique_group_permission (group_id, permission),
                    INDEX idx_group_permissions_group (group_id)
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
            statement.execute(createGroupPermissionsTable);  // ← ADD THIS
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

        permissionManager = new PermissionManager(this, dbManager);  // ← UPDATE THIS

        dbManager.loadAllGroupsIntoCache().thenRun(() -> {
            logger.info("Loaded " + dbManager.getAllGroups().size() + " groups into cache.");
        }).exceptionally(e -> {
            logger.warning("Failed to load groups into cache: " + e.getMessage());
            return null;
        });
    }

    private void registerCommands() {
        GroupSystemCommand command = new GroupSystemCommand(permissionManager, dbManager, this);
        GroupSystemTabCompleter tabCompleter = new GroupSystemTabCompleter(dbManager);

        Objects.requireNonNull(getCommand("gs")).setExecutor(command);
        Objects.requireNonNull(getCommand("gs")).setTabCompleter(tabCompleter);

        logger.info("Commands registered.");
    }

    private void registerEventListeners() {
        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new PlayerListener(dbManager, permissionManager, logger), this);
        pluginManager.registerEvents(new MotdListener(getConfig()), this);
        pluginManager.registerEvents(new SignListener(dbManager), this);

        logger.info("Event listeners registered.");
    }

    private void startExpiryCheckTask() {
        expiryCheckTask = new ExpiryCheckTask(this, dbManager, permissionManager)
                .runTaskTimerAsynchronously(this, 200L, 200L);

        logger.info("Expiry check task started (runs every 10 seconds)");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}