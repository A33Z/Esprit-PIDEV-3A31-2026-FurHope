package com.esprit.animal.Services;

import com.esprit.animal.entities.animal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Local, offline description generator based on templates.
 */
public class AIDescriptionService {

    public String generateDescription(String name, String species, String breed, int age, animal.gender gender) {
        String normalizedName = sanitize(name, "This pet");
        String normalizedSpecies = sanitize(species, "animal").toLowerCase(Locale.ROOT);
        String normalizedBreed = sanitize(breed, "lovely");
        String genderText = toGenderText(gender);

        List<String> templates = new ArrayList<>();
        templates.add(String.format(
                "%s is a %d year old %s %s looking for a loving home.",
                normalizedName, age, normalizedBreed, normalizedSpecies
        ));
        templates.add(String.format(
                "This %s %s %s named %s is %d years old and very friendly.",
                genderText, normalizedBreed, normalizedSpecies, normalizedName, age
        ));
        templates.add(String.format(
                "%s is a wonderful %s %s. At %d years old, this %s is ready to join a caring family.",
                normalizedName, normalizedBreed, normalizedSpecies, age, genderText
        ));

        String firstPart = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
        String secondPart = buildSecondSentence(normalizedSpecies, genderText, age);

        return firstPart + System.lineSeparator() + secondPart;
    }

    private String buildSecondSentence(String species, String genderText, int age) {
        String personality = pickPersonality(species, age);
        return String.format(
                "This %s %s is %s and waiting for a loving adopter.",
                genderText, species, personality
        );
    }

    private String pickPersonality(String species, int age) {
        String normalizedSpecies = species.toLowerCase(Locale.ROOT);
        boolean young = age <= 2;
        boolean senior = age >= 8;

        if (normalizedSpecies.contains("dog") || normalizedSpecies.contains("chien")) {
            if (young) {
                return "energetic, playful and sociable";
            }
            if (senior) {
                return "gentle, loyal and calm";
            }
            return "friendly, smart and affectionate";
        }

        if (normalizedSpecies.contains("cat") || normalizedSpecies.contains("chat")) {
            if (young) {
                return "curious, playful and affectionate";
            }
            if (senior) {
                return "calm, sweet and comforting";
            }
            return "calm, affectionate and charming";
        }

        if (young) {
            return "playful, curious and friendly";
        }
        if (senior) {
            return "calm, affectionate and easygoing";
        }
        return "friendly, gentle and social";
    }

    private String toGenderText(animal.gender gender) {
        if (gender == animal.gender.FEMALE) {
            return "female";
        }
        return "male";
    }

    private String sanitize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}

