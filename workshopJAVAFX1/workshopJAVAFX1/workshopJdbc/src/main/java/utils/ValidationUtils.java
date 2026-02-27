package utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Classe utilitaire servant à valider les champs saisis dans les formulaires.
 * Elle empêche l'utilisateur d'entrer des valeurs invalides ou vides
 * avant l'envoi des données vers la base.
 */
public final class ValidationUtils {

    // Constructeur privé : on ne veut pas instancier cette classe
    private ValidationUtils() {}

    /**
     * Vérifie qu'un texte n'est pas vide.
     * Exemple : ValidationUtils.requireText(nom, "Nom du vétérinaire");
     */
    public static String requireText(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        return value.trim();
    }

    /**
     * Vérifie qu'un nombre entier est positif (ex: ID, quantité...).
     * Lève une exception si le champ est vide ou non valide.
     */
    public static int parsePositiveInt(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldLabel + " doit être supérieur à 0.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " doit être un nombre entier valide.");
        }
    }

    /**
     * Vérifie et convertit une heure au format HH:mm (ex : 09:30).
     * Lève une erreur si le format est incorrect.
     */
    public static LocalTime parseHourMinute(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        try {
            return LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldLabel + " doit respecter le format HH:mm (ex : 08:45).");
        }
    }

    /**
     * Vérifie qu'un texte a une longueur minimale.
     * Exemple : requireMinLength(nom, "Nom", 3);
     */
    public static String requireMinLength(String value, String fieldLabel, int minLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        String normalized = value.trim();
        if (normalized.length() < minLength) {
            throw new IllegalArgumentException(fieldLabel + " doit contenir au moins " + minLength + " caractères.");
        }
        return normalized;
    }

    /**
     * Vérifie qu'une date est bien sélectionnée dans un DatePicker.
     */
    public static LocalDate requireDate(LocalDate date, String fieldLabel) {
        if (date == null) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        return date;
    }
}