package utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static int parsePositiveInt(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldLabel + " doit etre > 0.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " doit etre un nombre entier.");
        }
    }

    public static LocalTime parseHourMinute(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        try {
            return LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldLabel + " doit respecter le format HH:mm.");
        }
    }

    public static String requireMinLength(String value, String fieldLabel, int minLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        String normalized = value.trim();
        if (normalized.length() < minLength) {
            throw new IllegalArgumentException(fieldLabel + " doit contenir au moins " + minLength + " caracteres.");
        }
        return normalized;
    }

    public static LocalDate requireDate(LocalDate date, String fieldLabel) {
        if (date == null) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        return date;
    }
}
