package at.lukas.listener;

import at.lukas.manager.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Platzhalter:
 * - %PlayerName%
 * - %PlayerName%group%
 * <p>
 * Beispiele:
 * - %Luggez% → [Admin] Luggez
 * - %Steve% → [Member] Steve
 * - %Steve%group% → [Member]
 * - %Luggez%group% → [Admin]
 */

/**
 * Limitation: Schild platziert -> Groupwechsel -> Schild zeigt nocht alte Rolle
 */
public class SignListener implements Listener {
    private final DatabaseManager dbManager;

    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("%([A-Za-z0-9_]+)%");
    private static final Pattern PLAYER_GROUP_PATTERN = Pattern.compile("%([A-Za-z0-9_]+)%group%");

    public SignListener(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player placer = event.getPlayer();

        for (int i = 0; i < 4; i++) {
            Component line = event.line(i);
            if (line == null) continue;

            String plainText = LegacyComponentSerializer.legacySection().serialize(line);

            if (!plainText.contains("%")) {
                continue;
            }

            String processedText = processPlaceholders(plainText, placer);
            Component newLine = LegacyComponentSerializer.legacyAmpersand().deserialize(processedText);
            event.line(i, newLine);
        }
    }

    private String processPlaceholders(String text, Player placer) {
        String result = text;
        result = replaceGroupPlaceholders(result);
        result = replaceNamePlaceholders(result);
        return result;
    }

    private String replaceGroupPlaceholders(String text) {
        Matcher matcher = PLAYER_GROUP_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String playerName = matcher.group(1);
            String replacement = getPlayerGroupPrefix(playerName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String replaceNamePlaceholders(String text) {
        Matcher matcher = PLAYER_NAME_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String playerName = matcher.group(1);
            String replacement = getPlayerWithPrefix(playerName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String getPlayerWithPrefix(String playerName) {
        UUID playerUuid = getPlayerUUID(playerName);

        if (playerUuid == null) {
            return "&c[Unknown]";
        }

        String prefix = dbManager.getPlayerPrefix(playerUuid);
        if (prefix == null || prefix.isEmpty()) {
            prefix = "&7";
        }

        return prefix + playerName;
    }

    private String getPlayerGroupPrefix(String playerName) {
        UUID playerUuid = getPlayerUUID(playerName);

        if (playerUuid == null) {
            return "&c[Unknown]";
        }

        String prefix = dbManager.getPlayerPrefix(playerUuid);
        if (prefix != null && !prefix.isEmpty()) {
            return prefix;
        }

        return "&7[Member]";
    }

    private UUID getPlayerUUID(String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        }

        return null;
    }
}