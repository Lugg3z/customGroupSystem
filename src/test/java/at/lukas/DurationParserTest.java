package at.lukas;

import at.lukas.misc.DurationParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {
    @Test
    void parsesSingleUnitCorrectly() {
        assertEquals(1000L, DurationParser.parse("1s"));
        assertEquals(60_000L, DurationParser.parse("1m"));
        assertEquals(3_600_000L, DurationParser.parse("1h"));
        assertEquals(86_400_000L, DurationParser.parse("1d"));
        assertEquals(604_800_000L, DurationParser.parse("1w"));
        assertEquals(2_592_000_000L, DurationParser.parse("1mo"));
    }

    @Test
    void parsesMultipleUnitsCorrectly() {
        long expected =
                1L * 30 * 24 * 60 * 60 * 1000 +   // 1mo
                        2L * 7 * 24 * 60 * 60 * 1000 +    // 2w
                        3L * 24 * 60 * 60 * 1000 +        // 3d
                        4L * 60 * 60 * 1000 +             // 4h
                        5L * 60 * 1000 +                  // 5m
                        6L * 1000;                        // 6s

        assertEquals(expected, DurationParser.parse("1mo2w3d4h5m6s"));
    }

    @Test
    void parseIsCaseInsensitive() {
        assertEquals(DurationParser.parse("1H"), DurationParser.parse("1h"));
        assertEquals(DurationParser.parse("2Mo"), DurationParser.parse("2mo"));
    }

    @Test
    void returnsNullForPermanentKeywords() {
        assertNull(DurationParser.parse("permanent"));
        assertNull(DurationParser.parse("perm"));
        assertNull(DurationParser.parse("forever"));
        assertNull(DurationParser.parse("PERM"));
    }

    @Test
    void returnsNullForNullOrEmptyInput() {
        assertNull(DurationParser.parse(null));
        assertNull(DurationParser.parse(""));
    }

    @Test
    void throwsForInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parse("abc"));

        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parse("123"));

        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parse("mo1"));
    }

    @Test
    void throwsForZeroOrNegativeDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parse("0s"));
    }

    /* =========================
       formatDuration(long)
       ========================= */

    @Test
    void formatsExpiredDuration() {
        assertEquals("Expired", DurationParser.formatDuration(0));
        assertEquals("Expired", DurationParser.formatDuration(-1));
    }

    @Test
    void formatsSingleUnit() {
        assertEquals("1 second", DurationParser.formatDuration(1000));
        assertEquals("2 seconds", DurationParser.formatDuration(2000));
        assertEquals("1 minute", DurationParser.formatDuration(60_000));
        assertEquals("1 hour", DurationParser.formatDuration(3_600_000));
    }

    @Test
    void formatsMultipleUnits() {
        long millis =
                1L * 30 * 24 * 60 * 60 * 1000 +   // 1 month
                        1L * 7 * 24 * 60 * 60 * 1000 +    // 1 week
                        2L * 24 * 60 * 60 * 1000 +        // 2 days
                        3L * 60 * 60 * 1000 +             // 3 hours
                        4L * 60 * 1000 +                  // 4 minutes
                        5L * 1000;                        // 5 seconds

        String formatted = DurationParser.formatDuration(millis);

        assertEquals(
                "1 month, 1 week, 2 days, 3 hours, 4 minutes, 5 seconds",
                formatted
        );
    }

    /* =========================
       expiry helpers
       ========================= */

    @Test
    void expiryTimestampIsInFuture() {
        long duration = 5000;
        long before = System.currentTimeMillis();

        long expiry = DurationParser.getExpiryTimestamp(duration);

        assertTrue(expiry >= before + duration);
    }

    @Test
    void isExpiredWorksCorrectly() {
        long past = System.currentTimeMillis() - 1000;
        long future = System.currentTimeMillis() + 1000;

        assertTrue(DurationParser.isExpired(past));
        assertFalse(DurationParser.isExpired(future));
    }

    @Test
    void timeRemainingIsCorrect() {
        long future = System.currentTimeMillis() + 5000;

        long remaining = DurationParser.getTimeRemaining(future);

        assertTrue(remaining > 0);
        assertTrue(remaining <= 5000);
    }
}
