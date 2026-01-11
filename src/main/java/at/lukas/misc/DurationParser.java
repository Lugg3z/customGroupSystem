package at.lukas.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Übersetzt den Command "1mo2w3d4h5m6s" zu Millisekunden
 * <p>
 * Supported units:
 * - mo = months (30 days)
 * - w = weeks
 * - d = days
 * - h = hours
 * - m = minutes
 * - s = seconds
 */
public class DurationParser {
    private static final Pattern PATTERN = Pattern.compile(
            "(\\d+)(mo|w|d|h|m|s)"
    );

    public static Long parse(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        if (input.equalsIgnoreCase("permanent") ||
                input.equalsIgnoreCase("perm") ||
                input.equalsIgnoreCase("forever")) {
            return null;
        }

        long totalMillis = 0;
        Matcher matcher = PATTERN.matcher(input.toLowerCase());

        boolean foundAny = false;

        while (matcher.find()) {
            foundAny = true;

            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            totalMillis += switch (unit) {
                case "mo" -> value * 30L * 24 * 60 * 60 * 1000; // months (30 tage)
                case "w" -> value * 7L * 24 * 60 * 60 * 1000;   // weeks
                case "d" -> value * 24L * 60 * 60 * 1000;       // days
                case "h" -> value * 60L * 60 * 1000;            // hours
                case "m" -> value * 60L * 1000;                 // minutes
                case "s" -> value * 1000L;                      // seconds
                default -> throw new IllegalArgumentException("Unknown unit: " + unit);
            };
        }

        if (!foundAny) {
            throw new IllegalArgumentException("Invalid duration format: " + input);
        }

        if (totalMillis <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        return totalMillis;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "Expired";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;

        StringBuilder result = new StringBuilder();

        if (months > 0) {
            result.append(months).append(" month").append(months > 1 ? "s" : "");
            days -= months * 30;
        }

        if (weeks > 0) {
            if (!result.isEmpty()) result.append(", ");
            result.append(weeks).append(" week").append(weeks > 1 ? "s" : "");
            days -= weeks * 7;
        }

        if (days > 0) {
            if (!result.isEmpty()) result.append(", ");
            result.append(days).append(" day").append(days > 1 ? "s" : "");
        }

        hours %= 24;
        if (hours > 0) {
            if (!result.isEmpty()) result.append(", ");
            result.append(hours).append(" hour").append(hours > 1 ? "s" : "");
        }

        minutes %= 60;
        if (minutes > 0) {
            if (!result.isEmpty()) result.append(", ");
            result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }

        seconds %= 60;
        if (seconds > 0) {
            if (!result.isEmpty()) result.append(", ");
            result.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        }

        return result.toString();
    }

    public static long getExpiryTimestamp(long durationMillis) {
        return System.currentTimeMillis() + durationMillis;
    }

    public static boolean isExpired(long expiryTimestamp) {
        return System.currentTimeMillis() >= expiryTimestamp;
    }

    public static long getTimeRemaining(long expiryTimestamp) {
        return expiryTimestamp - System.currentTimeMillis();
    }
}