package at.lukas.commands;

import at.lukas.manager.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupSystemTabCompleter implements TabCompleter {

    private final DatabaseManager dbManager;

    public GroupSystemTabCompleter(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                    "creategroup",
                    "deletegroup",
                    "setpermission",
                    "setperm",
                    "listpermissions",
                    "listperms",
                    "adduser",
                    "playerinfo",
                    "pinfo"
            );

            return subcommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            return switch (subcommand) {
                case "deletegroup", "setpermission", "setperm", "listpermissions", "listperms" ->
                        getGroupCompletions(args[1]);
                case "adduser", "playerinfo", "pinfo" ->
                        getPlayerCompletions(args[1]);
                case "creategroup" ->
                        List.of("<n>");
                default ->
                        completions;
            };
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();

            return switch (subcommand) {
                case "creategroup" ->
                        List.of("<prefix>");
                case "adduser" ->
                        getGroupCompletions(args[2]);
                case "setpermission", "setperm" ->
                        getPermissionSuggestions(args[2]);
                default ->
                        completions;
            };
        }

        if (args.length == 4) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("setpermission") || subcommand.equals("setperm")) {
                return Stream.of("true", "false")
                        .filter(val -> val.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (subcommand.equals("adduser")) {
                return getDurationSuggestions(args[3]);
            }
        }

        return completions;
    }

    private List<String> getGroupCompletions(String partial) {
        try {
            List<String> groups = dbManager.getAllGroups();
            return groups.stream()
                    .filter(group -> group.toLowerCase().startsWith(partial.toLowerCase()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> getPlayerCompletions(String partial) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getDurationSuggestions(String partial) {
        List<String> suggestions = Arrays.asList(
                "1h",
                "3h",
                "12h",
                "1d",
                "3d",
                "7d",
                "14d",
                "1mo",
                "3mo",
                "permanent",
                "1d12h",
                "7d12h",
                "1mo2w",
                "2w3d"
        );

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getPermissionSuggestions(String partial) {
        List<String> suggestions = Arrays.asList(
                "minecraft.command.gamemode",
                "minecraft.command.tp",
                "minecraft.command.give",
                "minecraft.command.kill",
                "minecraft.command.kick",
                "minecraft.command.ban",
                "groupsystem.admin",
                "minecraft.command.*",
                "essentials.*",
                "worldedit.*"
        );

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
}