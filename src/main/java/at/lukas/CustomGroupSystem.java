package at.lukas;

import at.lukas.commands.GroupSystemCommand;
import at.lukas.commands.GroupSystemTabCompleter;
import at.lukas.misc.MotdListener;
import at.lukas.player.DatabaseManager;
import at.lukas.player.PermissionManager;
import at.lukas.player.PlayerListener;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;

public class CustomGroupSystem extends JavaPlugin {
    private final Logger logger = getLogger();
    private HikariDataSource dataSource;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            connectToDatabase();
            createTablesOnFirstBoot();
        } catch (SQLException e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
        PermissionManager permissionManager = new PermissionManager();
        DatabaseManager dbManager = new DatabaseManager();

        GroupSystemCommand command = new GroupSystemCommand(permissionManager, dbManager, this);
        Objects.requireNonNull(getCommand("gs")).setExecutor(command);

        GroupSystemTabCompleter tabCompleter = new GroupSystemTabCompleter(dbManager);
        Objects.requireNonNull(getCommand("gs")).setTabCompleter(tabCompleter);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(dbManager), this);
        pm.registerEvents(new MotdListener(getConfig()), this);
    }

    @Override
    public void onDisable() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
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
                logger.info("Successfully connected to database!");
            }
        }
    }

    private void createTablesOnFirstBoot() throws SQLException {
        String createRolesTable = """
                CREATE TABLE IF NOT EXISTS roles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(36) NOT NULL UNIQUE,
                    prefix VARCHAR(8) UNIQUE
                )
                """;

        String createPlayerRolesTable = """
                CREATE TABLE IF NOT EXISTS player_roles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid CHAR(36) NOT NULL,
                    role_id INT NOT NULL,
                    expiry DATETIME NULL,
                    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_player_roles_role
                        FOREIGN KEY (role_id)
                        REFERENCES roles(id)
                        ON DELETE CASCADE,
                    INDEX idx_player_roles_uuid (uuid),
                    INDEX idx_player_roles_role (role_id),
                    INDEX idx_player_roles_expiry (expiry)
                )
                """;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createRolesTable);
            statement.execute(createPlayerRolesTable);

            logger.info("Database tables initialized successfully.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}