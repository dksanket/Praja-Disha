package gov.prajadisha.backend.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared date/time formatting used to shape response payloads for the frontends.
 */
public final class Formats {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter FULL_DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private Formats() {}

    /** e.g. "Oct 30, 2026" */
    public static String fullDate(long epochMillis) {
        return FULL_DATE.format(Instant.ofEpochMilli(epochMillis).atZone(ZONE));
    }

    /** e.g. "Oct 30, 2026, 4:15 PM" */
    public static String dateTime(long epochMillis) {
        return DATE_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZONE));
    }

    /** "Today" if same calendar day, else "Oct 24". */
    public static String shortOrToday(long epochMillis) {
        LocalDate day = Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate();
        if (day.equals(LocalDate.now(ZONE))) return "Today";
        return SHORT_DATE.format(day);
    }

    public static boolean isToday(long epochMillis) {
        LocalDate day = Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate();
        return day.equals(LocalDate.now(ZONE));
    }
}
