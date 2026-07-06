package gov.prajadisha.backend.common;

import java.text.Normalizer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Helpers for generating business-friendly identifiers and slugs.
 */
public final class Ids {

    private Ids() {}

    /** Ticket ids look like "PD-8821". */
    public static String ticketId() {
        return "PD-" + (1000 + ThreadLocalRandom.current().nextInt(9000));
    }

    public static String prefixed(String prefix) {
        return prefix + "-" + (100 + ThreadLocalRandom.current().nextInt(900));
    }

    /** Turn a display name into a snake_case username, e.g. "Aisha Patel" -> "aisha_patel". */
    public static String usernameFromName(String name) {
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "citizen_" + ThreadLocalRandom.current().nextInt(100000) : normalized;
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
