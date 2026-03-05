package com.esprit.animal.Services;

import com.esprit.animal.entities.AnimalCareAdvice;
import com.esprit.animal.entities.animal;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Offline rule-based recommendation engine for animal care advice.
 * Uses species, breed and age rules to simulate AI-like recommendations.
 */
public class AnimalCareRecommendationService {

    private static final Map<String, AdviceTemplate> SPECIES_RULES = new LinkedHashMap<>();
    private static final Map<String, AdviceTemplate> BREED_RULES = new LinkedHashMap<>();

    static {
        registerSpeciesRules();
        registerBreedRules();
    }

    public AnimalCareAdvice generateCareAdvice(animal pet) {
        if (pet == null) {
            return buildFallbackAdvice();
        }

        return generateCareAdvice(pet.getSpecies(), pet.getBreed(), pet.getAge(), pet.getGender());
    }

    public AnimalCareAdvice generateCareAdvice(String species, String breed, int age, animal.gender gender) {
        AdviceTemplate combined = AdviceTemplate.defaultTemplate();

        AdviceTemplate speciesTemplate = resolveSpeciesTemplate(species);
        combined.merge(speciesTemplate);

        AdviceTemplate breedTemplate = resolveBreedTemplate(breed);
        combined.merge(breedTemplate);

        appendAdvice(combined, resolveAgeTemplate(age));

        AdviceTemplate genderTemplate = resolveGenderTemplate(gender);
        if (genderTemplate.training != null && !genderTemplate.training.isBlank()) {
            combined.training = combined.training + " " + genderTemplate.training;
        }

        AnimalCareAdvice advice = new AnimalCareAdvice();
        advice.setExerciseRecommendation(combined.exercise);
        advice.setDietRecommendation(combined.diet);
        advice.setEnvironmentRecommendation(combined.environment);
        advice.setGroomingRecommendation(combined.grooming);
        advice.setTrainingRecommendation(combined.training);
        return advice;
    }

    private void appendAdvice(AdviceTemplate base, AdviceTemplate additions) {
        if (additions == null) {
            return;
        }

        if (additions.exercise != null && !additions.exercise.isBlank()) {
            base.exercise = base.exercise + " " + additions.exercise;
        }
        if (additions.diet != null && !additions.diet.isBlank()) {
            base.diet = base.diet + " " + additions.diet;
        }
        if (additions.environment != null && !additions.environment.isBlank()) {
            base.environment = base.environment + " " + additions.environment;
        }
        if (additions.grooming != null && !additions.grooming.isBlank()) {
            base.grooming = base.grooming + " " + additions.grooming;
        }
        if (additions.training != null && !additions.training.isBlank()) {
            base.training = base.training + " " + additions.training;
        }
    }

    private AdviceTemplate resolveSpeciesTemplate(String species) {
        String normalized = normalize(species);
        for (Map.Entry<String, AdviceTemplate> entry : SPECIES_RULES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return AdviceTemplate.empty();
    }

    private AdviceTemplate resolveBreedTemplate(String breed) {
        String normalized = normalize(breed);
        for (Map.Entry<String, AdviceTemplate> entry : BREED_RULES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return AdviceTemplate.empty();
    }

    private AdviceTemplate resolveAgeTemplate(int age) {
        if (age < 1) {
            return new AdviceTemplate(
                    "Short and frequent play sessions are better than intense exercise for young animals.",
                    "Provide age-appropriate food in multiple small meals each day and ensure hydration.",
                    "Keep a safe and supervised environment, avoid hazards, and limit stressful exposure.",
                    "Use gentle handling and short grooming sessions to build positive habits early.",
                    "Focus on gentle training, early socialization, and frequent positive reinforcement."
            );
        }

        if (age > 7) {
            return new AdviceTemplate(
                    "Prefer moderate, low-impact activity with rest periods to protect joints.",
                    "Use a senior-focused balanced diet and monitor weight regularly.",
                    "Provide a calm, accessible environment with comfortable resting areas.",
                    "Increase coat and skin checks; adapt grooming frequency to comfort and health needs.",
                    "Use low-stress routines and short refresh sessions rather than intensive training."
            );
        }

        return new AdviceTemplate(
                "Maintain daily physical and mental activity adjusted to the animal's energy level.",
                "Provide a balanced diet with consistent portions and regular feeding times.",
                "Ensure a stable environment with enough enrichment and social interaction.",
                "Maintain routine grooming and monitor skin, ears, nails, and coat condition.",
                "Use consistent positive training and enrichment to reinforce good behavior."
        );
    }

    private AdviceTemplate resolveGenderTemplate(animal.gender gender) {
        if (gender == null) {
            return AdviceTemplate.empty();
        }

        String trainingNote = "Plan regular veterinary follow-up and discuss preventive reproductive care.";
        if (gender == animal.gender.MALE) {
            trainingNote = "Provide structure and social boundaries, and discuss preventive reproductive care with a vet.";
        } else if (gender == animal.gender.FEMALE) {
            trainingNote = "Keep routines stable and discuss preventive reproductive care and hormonal cycle management with a vet.";
        }

        return new AdviceTemplate(null, null, null, null, trainingNote);
    }

    private AnimalCareAdvice buildFallbackAdvice() {
        AnimalCareAdvice advice = new AnimalCareAdvice();
        AdviceTemplate base = AdviceTemplate.defaultTemplate();
        advice.setExerciseRecommendation(base.exercise);
        advice.setDietRecommendation(base.diet);
        advice.setEnvironmentRecommendation(base.environment);
        advice.setGroomingRecommendation(base.grooming);
        advice.setTrainingRecommendation(base.training);
        return advice;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String noDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return noDiacritics.toLowerCase(Locale.ROOT).trim();
    }

    private static void registerSpeciesRules() {
        AdviceTemplate dogAdvice = new AdviceTemplate(
                "Dogs generally require daily exercise with walks and interactive play.",
                "Provide a quality diet rich in protein with portions adjusted to activity level.",
                "Dogs do best with consistent routines, enrichment, and secure space for movement.",
                "Brush regularly, check ears and paws, and maintain nail care.",
                "Use consistent positive training and early socialization."
        );
        SPECIES_RULES.put("dog", dogAdvice);
        SPECIES_RULES.put("chien", dogAdvice);
        SPECIES_RULES.put("puppy", dogAdvice);

        AdviceTemplate catAdvice = new AdviceTemplate(
                "Cats need moderate daily activity with climbing, toys, and play sessions.",
                "Serve balanced feline nutrition and ensure constant access to fresh water.",
                "Indoor-focused environments are recommended, with scratching posts and quiet zones.",
                "Brush coat regularly and monitor hairballs, nails, and litter hygiene.",
                "Use short reward-based sessions for litter habits and behavioral guidance."
        );
        SPECIES_RULES.put("cat", catAdvice);
        SPECIES_RULES.put("chat", catAdvice);
        SPECIES_RULES.put("kitten", catAdvice);

        AdviceTemplate rabbitAdvice = new AdviceTemplate(
                "Rabbits need daily supervised movement and space outside the cage.",
                "Base diet on hay, fresh vegetables, and controlled pellets.",
                "Provide a calm indoor environment with clean bedding and safe chew toys.",
                "Brush weekly and check nails and teeth regularly.",
                "Use gentle handling and trust-building routines; avoid harsh correction."
        );
        SPECIES_RULES.put("rabbit", rabbitAdvice);
        SPECIES_RULES.put("lapin", rabbitAdvice);
        SPECIES_RULES.put("bunny", rabbitAdvice);

        AdviceTemplate birdAdvice = new AdviceTemplate(
                "Birds need daily flight or movement opportunities and mental stimulation.",
                "Use species-appropriate seeds/pellets with fresh vegetables and clean water.",
                "Provide a spacious cage, natural perches, and social interaction.",
                "Keep feathers clean and monitor beak and nail condition.",
                "Reinforce routines with positive interaction and vocal/social stimulation."
        );
        SPECIES_RULES.put("bird", birdAdvice);
        SPECIES_RULES.put("oiseau", birdAdvice);
        SPECIES_RULES.put("parrot", birdAdvice);

        AdviceTemplate reptileAdvice = new AdviceTemplate(
                "Reptiles need controlled activity and species-specific handling frequency.",
                "Diet depends on species; ensure calcium, vitamins, and hydration support.",
                "Maintain strict temperature and humidity gradients with UVB when required.",
                "Monitor shedding quality and maintain enclosure hygiene.",
                "Use calm handling routines to reduce stress."
        );
        SPECIES_RULES.put("reptile", reptileAdvice);
        SPECIES_RULES.put("reptiles", reptileAdvice);
        SPECIES_RULES.put("serpent", reptileAdvice);
        SPECIES_RULES.put("lezard", reptileAdvice);
        SPECIES_RULES.put("lizard", reptileAdvice);

        AdviceTemplate fishAdvice = new AdviceTemplate(
                "Fish activity depends on water quality and tank size rather than direct exercise.",
                "Feed small portions and avoid overfeeding to protect water quality.",
                "Maintain stable aquarium parameters and adequate filtration.",
                "No direct grooming needed; monitor fins, scales, and behavior closely.",
                "Training is limited; focus on stable routines and stress-free tank management."
        );
        SPECIES_RULES.put("fish", fishAdvice);
        SPECIES_RULES.put("poisson", fishAdvice);
        SPECIES_RULES.put("aquarium", fishAdvice);

        AdviceTemplate hamsterAdvice = new AdviceTemplate(
                "Hamsters need daily movement wheels and safe exploration time.",
                "Provide balanced small-mammal feed and occasional fresh vegetables.",
                "Use a quiet environment with deep bedding and enrichment tunnels.",
                "Minimal grooming is needed, but regular habitat cleaning is essential.",
                "Use gentle, short handling sessions to build trust."
        );
        SPECIES_RULES.put("hamster", hamsterAdvice);
    }

    private static void registerBreedRules() {
        BREED_RULES.put("husky", new AdviceTemplate(
                "Huskies require high daily physical activity and strong mental stimulation.",
                "High-quality protein-rich nutrition is recommended for this active breed.",
                "Best suited for cool environments and homes with outdoor space.",
                "Frequent brushing is needed, especially during heavy shedding periods.",
                "Early obedience training is essential because Huskies are intelligent and independent."
        ));

        BREED_RULES.put("golden retriever", new AdviceTemplate(
                "Golden Retrievers need regular exercise and interactive games.",
                "Balanced diet with controlled portions helps prevent excess weight.",
                "They thrive in social family environments with daily interaction.",
                "Brush several times per week to maintain coat health.",
                "Consistent social and obedience training works very well with this breed."
        ));

        BREED_RULES.put("persian", new AdviceTemplate(
                "Persian cats need moderate indoor activity and interactive play.",
                "Feed a balanced cat diet and monitor hydration closely.",
                "Indoor lifestyle is strongly recommended for comfort and coat protection.",
                "Daily brushing is important to prevent matting of long fur.",
                "Use calm routines and gentle reinforcement."
        ));

        BREED_RULES.put("bearded dragon", new AdviceTemplate(
                "Provide light daily stimulation and safe supervised movement outside enclosure.",
                "Offer species-appropriate insects and vegetables with calcium supplementation.",
                "A controlled heated habitat with UVB lighting is mandatory.",
                "Support proper shedding with suitable humidity and substrate hygiene.",
                "Minimize stress with predictable handling and enclosure routines."
        ));
    }

    private static final class AdviceTemplate {
        private String exercise;
        private String diet;
        private String environment;
        private String grooming;
        private String training;

        private AdviceTemplate(String exercise, String diet, String environment, String grooming, String training) {
            this.exercise = exercise;
            this.diet = diet;
            this.environment = environment;
            this.grooming = grooming;
            this.training = training;
        }

        private static AdviceTemplate empty() {
            return new AdviceTemplate(null, null, null, null, null);
        }

        private static AdviceTemplate defaultTemplate() {
            return new AdviceTemplate(
                    "Provide regular daily activity adapted to this animal's condition.",
                    "Offer a balanced species-appropriate diet with fresh water.",
                    "Maintain a clean, safe, and stress-free living environment.",
                    "Follow a consistent grooming and hygiene routine.",
                    "Use positive reinforcement and regular routines for behavior support."
            );
        }

        private void merge(AdviceTemplate other) {
            if (other == null) {
                return;
            }

            if (other.exercise != null && !other.exercise.isBlank()) {
                this.exercise = other.exercise;
            }
            if (other.diet != null && !other.diet.isBlank()) {
                this.diet = other.diet;
            }
            if (other.environment != null && !other.environment.isBlank()) {
                this.environment = other.environment;
            }
            if (other.grooming != null && !other.grooming.isBlank()) {
                this.grooming = other.grooming;
            }
            if (other.training != null && !other.training.isBlank()) {
                this.training = other.training;
            }
        }
    }
}

