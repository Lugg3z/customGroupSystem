package at.lukas.commands;

import at.lukas.misc.DurationParser;
import at.lukas.player.DatabaseManager;
import at.lukas.player.PermissionManager;
import at.lukas.player.PlayerHelper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import static java.util.logging.Logger.getLogger;

public class GroupSystemCommand implements CommandExecutor {
    private final Logger logger = getLogger(GroupSystemCommand.class.getName());

    private final PermissionManager permManager;
    private final DatabaseManager dbManager;
    private final Plugin plugin;
    private FileConfiguration messages;

    public GroupSystemCommand(PermissionManager permManager, DatabaseManager dbManager, Plugin plugin) {
        this.permManager = permManager;
        this.dbManager = dbManager;
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                } else {
                    messagesFile.createNewFile();
                    logger.warning("messages.yml not found in resources, created empty file");
                }
            } catch (IOException e) {
                logger.severe("Could not create messages.yml: " + e.getMessage());
            }
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private String getMessage(String path, Object... placeholders) {
        String message = messages.getString("messages." + path, "§cMessage not found: " + path);

        // Replace placeholders
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = "{" + placeholders[i] + "}";
                String value = String.valueOf(placeholders[i + 1]);
                message = message.replace(placeholder, value);
            }
        }

        return message;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /gs no arguments
        if (args.length == 0) {
            sender.sendMessage(getMessage("help.header"));
            sender.sendMessage(getMessage("help.creategroup"));
            sender.sendMessage(getMessage("help.deletegroup"));
            sender.sendMessage(getMessage("help.setpermission"));
            sender.sendMessage(getMessage("help.adduser"));
            sender.sendMessage(getMessage("help.playerinfo"));
            return true;
        }

        String subcommand = args[0].toLowerCase();
        return switch (subcommand) {
            case "creategroup" -> handleCreateGroup(sender, args);
            case "deletegroup" -> handleDeleteGroup(sender, args);
            /*
            case "setpermission" -> handleSetPermission(sender, args);
            */
            case "adduser" -> handleAddUser(sender, args);
            case "playerinfo", "pinfo" -> handlePlayerInfo(sender, args);
            default -> {
                sender.sendMessage(getMessage("unknown-subcommand", "subcommand", subcommand));
                yield true;
            }
        };
    }

    // /gs creategroup <name> <prefix>
    private boolean handleCreateGroup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("groupsystem.admin.creategroup")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(getMessage("creategroup.usage"));
            return true;
        }

        String groupName = args[1];
        String prefix = args[2];

        if (args.length > 3) {
            sender.sendMessage(getMessage("creategroup.too-many-args"));
            return true;
        }

        try {
            if (dbManager.groupExists(groupName)) {
                sender.sendMessage(getMessage("creategroup.already-exists", "group", groupName));
                return true;
            }

            dbManager.createGroup(groupName, prefix);
            sender.sendMessage(getMessage("creategroup.success", "group", groupName));

        } catch (Exception e) {
            sender.sendMessage(getMessage("creategroup.error", "error", e.getMessage()));
            logger.severe("Error creating group: " + e.getMessage());
        }

        return true;
    }

    // /gs deletegroup <name>
    private boolean handleDeleteGroup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("groupsystem.admin.deletegroup")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(getMessage("deletegroup.usage"));
            return true;
        }

        String groupName = args[1];

        try {
            if (!dbManager.groupExists(groupName)) {
                sender.sendMessage(getMessage("deletegroup.not-found", "group", groupName));
                return true;
            }

            if (!dbManager.deleteGroup(groupName)) {
                sender.sendMessage(getMessage("deletegroup.failed", "group", groupName));
            } else {
                sender.sendMessage(getMessage("deletegroup.success", "group", groupName));
            }

        } catch (Exception e) {
            sender.sendMessage(getMessage("deletegroup.error", "error", e.getMessage()));
            logger.severe("Error deleting group: " + e.getMessage());
        }

        return true;
    }

    // /gs adduser <player> <group> [duration]
    private boolean handleAddUser(CommandSender sender, String[] args) {
        if (!sender.hasPermission("groupsystem.admin.adduser")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(getMessage("adduser.usage"));
            sender.sendMessage(getMessage("adduser.duration-examples"));
            return true;
        }

        String playerName = args[1];
        String groupName = args[2].toLowerCase();

        Long expiryMillis = null;
        String durationStr = null;

        if (args.length >= 4) {
            durationStr = args[3];

            try {
                Long durationMillis = DurationParser.parse(durationStr);
                if (durationMillis != null) {
                    expiryMillis = DurationParser.getExpiryTimestamp(durationMillis);
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(getMessage("adduser.invalid-duration", "error", e.getMessage()));
                sender.sendMessage(getMessage("adduser.duration-examples"));
                return true;
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        UUID uuid;

        if (target != null) {
            uuid = target.getUniqueId();
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore()) {
                sender.sendMessage(getMessage("adduser.player-not-found", "player", playerName));
                return true;
            }
            uuid = offlinePlayer.getUniqueId();
        }

        try {
            if (!dbManager.groupExists(groupName)) {
                sender.sendMessage(getMessage("adduser.group-not-found", "group", groupName));
                return true;
            }

            dbManager.setUserGroup(uuid, groupName, expiryMillis);

            sender.sendMessage(getMessage("adduser.success", "player", playerName, "group", groupName));

            if (expiryMillis == null) {
                sender.sendMessage(getMessage("adduser.duration-permanent"));
            } else {
                long remaining = expiryMillis - System.currentTimeMillis();
                String formatted = DurationParser.formatDuration(remaining);
                sender.sendMessage(getMessage("adduser.duration-temporary", "duration", formatted));

                String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(expiryMillis));
                sender.sendMessage(getMessage("adduser.expires-at", "date", date));
            }

            if (target != null) {
                PlayerHelper.applyPrefix(target, dbManager);
            } else {
                sender.sendMessage(getMessage("adduser.player-offline"));
            }

        } catch (SQLException e) {
            sender.sendMessage(getMessage("adduser.error", "error", e.getMessage()));
            plugin.getLogger().severe("Error adding user: " + e.getMessage());
        }

        return true;
    }

    // /gs playerinfo <player>
    private boolean handlePlayerInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("groupsystem.admin.playerinfo")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getMessage("playerinfo.usage"));
            return true;
        }

        String playerName = args[1];

        Player target = Bukkit.getPlayer(playerName);
        UUID uuid;
        String displayName;

        if (target != null) {
            uuid = target.getUniqueId();
            displayName = target.getName();
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore()) {
                sender.sendMessage(getMessage("playerinfo.player-not-found", "player", playerName));
                return true;
            }
            uuid = offlinePlayer.getUniqueId();
            displayName = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerName;
        }

        try {
            String groupName = dbManager.getPlayerGroup(uuid);
            String prefix = dbManager.getPlayerPrefix(uuid);
            Long expiryMillis = dbManager.getPlayerGroupExpiry(uuid);

            sender.sendMessage(getMessage("playerinfo.header", "player", displayName));
            sender.sendMessage(getMessage("playerinfo.uuid", "uuid", uuid.toString()));
            sender.sendMessage(getMessage("playerinfo.group", "group", groupName));
            sender.sendMessage(getMessage("playerinfo.prefix", "prefix", prefix, "player", displayName));

            if (expiryMillis == null) {
                sender.sendMessage(getMessage("playerinfo.duration-permanent"));
            } else {
                long remaining = expiryMillis - System.currentTimeMillis();

                if (remaining <= 0) {
                    sender.sendMessage(getMessage("playerinfo.duration-expired"));
                } else {
                    String formatted = DurationParser.formatDuration(remaining);
                    sender.sendMessage(getMessage("playerinfo.time-remaining", "time", formatted));

                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(expiryMillis));
                    sender.sendMessage(getMessage("playerinfo.expires", "date", date));
                }
            }

            sender.sendMessage(target != null ? getMessage("playerinfo.status-online") : getMessage("playerinfo.status-offline"));

        } catch (SQLException e) {
            sender.sendMessage(getMessage("playerinfo.error", "error", e.getMessage()));
            plugin.getLogger().severe("Error getting player info: " + e.getMessage());
        }

        return true;
    }
}