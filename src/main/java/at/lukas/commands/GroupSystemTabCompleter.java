package at.lukas.commands;

import at.lukas.player.DatabaseManager;
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

//Von Claude mit Basis von meinem Code
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
                    "adduser"
            );

            return subcommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            return switch (subcommand) {
                case "deletegroup", "setpermission" -> getGroupCompletions(args[1]);
                case "adduser" -> getPlayerCompletions(args[1]);
                case "creategroup" -> List.of("<name>");
                default -> completions;
            };
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();

            return switch (subcommand) {
                case "creategroup" -> List.of("<prefix>");
                case "adduser" -> getGroupCompletions(args[2]);
                case "setpermission" -> List.of("<permission>");
                default -> completions;
            };
        }

        if (args.length == 4) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("setpermission")) {
                return Stream.of("true", "false")
                        .filter(val -> val.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
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
}