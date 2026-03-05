package com.esprit.animal.Services;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Local offline recognition service based on filename keyword analysis.
 * Structured to stay extensible for future real ML integrations.
 */
public class AutoRecognitionService {

    private static final Map<String, SpeciesProfile> SPECIES_DATABASE = new LinkedHashMap<>();

    static {
        registerDefaultProfiles();
    }

    private AutoRecognitionService() {
    }

    public static synchronized void registerCustomSpecies(
            String key,
            String displayName,
            List<String> keywords,
            List<String> breeds
    ) {
        registerSpecies(key, displayName, keywords, breeds, Collections.emptyMap());
    }

    public static synchronized void registerCustomSpecies(
            String key,
            String displayName,
            List<String> keywords,
            List<String> breeds,
            Map<String, List<String>> breedAliasesByBreed
    ) {
        registerSpecies(key, displayName, keywords, breeds, breedAliasesByBreed);
    }

    public static List<String> getSupportedSpecies() {
        List<String> species = new ArrayList<>();
        for (SpeciesProfile profile : SPECIES_DATABASE.values()) {
            species.add(profile.displayName);
        }
        return Collections.unmodifiableList(species);
    }

    public static AnimalRecognitionResult analyzeImage(File imageFile) {
        AnimalRecognitionResult result = new AnimalRecognitionResult();
        if (imageFile == null) {
            result.setSpecies("Unknown");
            result.setBreed("Unknown");
            result.setConfidence(randomBetween(0.30f, 0.50f));
            result.setImagePath(null);
            return result;
        }

        String normalizedName = normalize(imageFile.getName());
        Set<String> tokens = tokenize(normalizedName);
        MatchResult match = detectBestSpecies(normalizedName, tokens);

        if (match == null) {
            result.setSpecies("Unknown");
            result.setBreed("Unknown");
            result.setConfidence(randomBetween(0.30f, 0.50f));
            result.setImagePath(imageFile.getAbsolutePath());
            return result;
        }

        result.setSpecies(match.profile.displayName);

        if (match.detectedBreed != null) {
            result.setBreed(match.detectedBreed);
            result.setConfidence(calculateBreedConfidence(match));
        } else {
            result.setBreed(getRandomBreed(match.profile));
            result.setConfidence(calculateSpeciesOnlyConfidence(match));
        }

        result.setImagePath(imageFile.getAbsolutePath());
        return result;
    }

    private static MatchResult detectBestSpecies(String normalizedFilename, Set<String> tokens) {
        MatchResult best = null;

        for (SpeciesProfile profile : SPECIES_DATABASE.values()) {
            BreedMatch breedMatch = findBreedMatch(profile, normalizedFilename, tokens);
            int keywordHits = countKeywordHits(profile.keywords, normalizedFilename, tokens);

            if (breedMatch == null && keywordHits <= 0) {
                continue;
            }

            MatchResult current = new MatchResult(
                    profile,
                    breedMatch == null ? null : breedMatch.breed,
                    keywordHits,
                    breedMatch == null ? 0 : breedMatch.aliasScore
            );

            if (best == null || current.isBetterThan(best)) {
                best = current;
            }
        }

        return best;
    }

    private static int countKeywordHits(Set<String> keywords, String normalizedFilename, Set<String> tokens) {
        int hits = 0;
        for (String keyword : keywords) {
            if (containsToken(normalizedFilename, tokens, keyword)) {
                hits++;
            }
        }
        return hits;
    }

    private static BreedMatch findBreedMatch(SpeciesProfile profile, String normalizedFilename, Set<String> tokens) {
        String bestBreed = null;
        int bestAliasScore = -1;

        for (Map.Entry<String, String> entry : profile.breedAliases.entrySet()) {
            String alias = entry.getKey();
            if (!containsToken(normalizedFilename, tokens, alias)) {
                continue;
            }

            int aliasScore = alias.replace(" ", "").length();
            if (aliasScore > bestAliasScore) {
                bestAliasScore = aliasScore;
                bestBreed = entry.getValue();
            }
        }

        if (bestBreed == null) {
            return null;
        }
        return new BreedMatch(bestBreed, bestAliasScore);
    }

    private static String getRandomBreed(SpeciesProfile profile) {
        if (profile.breeds.isEmpty()) {
            return "Unknown";
        }
        int idx = ThreadLocalRandom.current().nextInt(profile.breeds.size());
        return profile.breeds.get(idx);
    }

    private static float calculateBreedConfidence(MatchResult match) {
        float keywordBonus = Math.min(0.025f, match.keywordHits * 0.006f);
        float aliasBonus = Math.min(0.025f, match.bestBreedAliasScore * 0.001f);
        return clamp(0.95f + keywordBonus + aliasBonus, 0.95f, 1.00f);
    }

    private static float calculateSpeciesOnlyConfidence(MatchResult match) {
        float keywordBonus = Math.min(0.15f, match.keywordHits * 0.03f);
        return clamp(0.75f + keywordBonus, 0.75f, 0.90f);
    }

    private static float randomBetween(float min, float max) {
        return (float) (min + (ThreadLocalRandom.current().nextDouble() * (max - min)));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean containsToken(String normalizedFilename, Set<String> tokens, String normalizedToken) {
        if (normalizedFilename == null || normalizedFilename.isBlank() || normalizedToken == null || normalizedToken.isBlank()) {
            return false;
        }

        if (tokens.contains(normalizedToken)) {
            return true;
        }

        String filename = " " + normalizedFilename + " ";
        String token = " " + normalizedToken + " ";
        if (filename.contains(token)) {
            return true;
        }

        String compactName = normalizedFilename.replace(" ", "");
        String compactToken = normalizedToken.replace(" ", "");
        return compactToken.length() >= 4 && compactName.contains(compactToken);
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }

        String noDiacritics = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return noDiacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\.[a-z0-9]{2,5}$", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static Set<String> tokenize(String normalized) {
        Set<String> tokens = new LinkedHashSet<>();
        if (normalized == null || normalized.isBlank()) {
            return tokens;
        }

        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            tokens.add(token);

            if (token.endsWith("s") && token.length() > 3) {
                tokens.add(token.substring(0, token.length() - 1));
            }
        }
        return tokens;
    }

    private static synchronized void registerSpecies(
            String key,
            String displayName,
            List<String> keywords,
            List<String> breeds,
            Map<String, List<String>> breedAliasesByBreed
    ) {
        SpeciesProfile profile = new SpeciesProfile(normalizeKey(key), displayName);

        for (String keyword : keywords) {
            profile.addKeyword(keyword);
        }

        Map<String, List<String>> aliasesMap = breedAliasesByBreed == null ? Collections.emptyMap() : breedAliasesByBreed;
        for (String breed : breeds) {
            profile.addBreed(breed);

            List<String> aliases = aliasesMap.get(breed);
            if (aliases == null) {
                continue;
            }
            for (String alias : aliases) {
                profile.addBreedAlias(breed, alias);
            }
        }

        SPECIES_DATABASE.put(profile.key, profile);
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "CUSTOM_" + System.nanoTime();
        }
        return key.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static Map<String, List<String>> aliasMap(AliasEntry... entries) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (AliasEntry entry : entries) {
            map.put(entry.breed, Arrays.asList(entry.aliases));
        }
        return map;
    }

    private static AliasEntry alias(String breed, String... aliases) {
        return new AliasEntry(breed, aliases);
    }

    private static void registerDefaultProfiles() {
        registerSpecies(
                "DOG",
                "Dog",
                Arrays.asList(
                        "dog", "dogs", "chien", "chiot", "puppy", "canine", "husky", "labrador", "shepherd",
                        "retriever", "akita", "shiba", "pitbull", "malamute", "collie", "rottweiler", "beagle"
                ),
                Arrays.asList(
                        "Labrador Retriever", "Golden Retriever", "German Shepherd", "Border Collie", "Australian Shepherd",
                        "Siberian Husky", "Alaskan Malamute", "Akita", "Shiba Inu", "Great Dane", "Pitbull",
                        "Chow Chow", "Poodle", "Beagle", "Boxer", "Rottweiler", "Doberman", "Dalmatian",
                        "Corgi", "French Bulldog", "Bulldog", "Jack Russell Terrier", "Samoyed", "Cane Corso"
                ),
                aliasMap(
                        alias("German Shepherd", "german shepherd", "shepherd", "berger allemand", "gsd"),
                        alias("Siberian Husky", "husky", "siberian husky"),
                        alias("Labrador Retriever", "labrador", "lab"),
                        alias("Golden Retriever", "golden retriever", "golden"),
                        alias("French Bulldog", "french bulldog", "frenchie"),
                        alias("Jack Russell Terrier", "jack russell", "jrt"),
                        alias("Pitbull", "pitbull", "pit bull", "american pit bull"),
                        alias("Australian Shepherd", "australian shepherd", "aussie"),
                        alias("Border Collie", "border collie", "collie"),
                        alias("Shiba Inu", "shiba", "shiba inu")
                )
        );

        registerSpecies(
                "CAT",
                "Cat",
                Arrays.asList(
                        "cat", "cats", "chat", "chaton", "kitten", "feline", "persian", "siamese", "ragdoll",
                        "maine coon", "bengal", "british shorthair", "scottish fold"
                ),
                Arrays.asList(
                        "Persian", "Siamese", "Ragdoll", "Maine Coon", "Bengal", "Scottish Fold", "Savannah",
                        "Ocicat", "Turkish Van", "Norwegian Forest Cat", "American Shorthair", "British Shorthair",
                        "Sphynx", "Abyssinian"
                ),
                aliasMap(
                        alias("Persian", "persian", "persan"),
                        alias("Siamese", "siamese", "siamois"),
                        alias("Ragdoll", "ragdoll"),
                        alias("Maine Coon", "maine coon"),
                        alias("British Shorthair", "british shorthair"),
                        alias("American Shorthair", "american shorthair"),
                        alias("Scottish Fold", "scottish fold"),
                        alias("Norwegian Forest Cat", "norwegian forest", "forest cat"),
                        alias("Turkish Van", "turkish van"),
                        alias("Savannah", "savannah")
                )
        );

        registerSpecies(
                "RABBIT",
                "Rabbit",
                Arrays.asList(
                        "rabbit", "rabbits", "lapin", "lapine", "bunny", "hare", "lop", "rex", "angora"
                ),
                Arrays.asList(
                        "Holland Lop", "Mini Rex", "Lionhead", "Flemish Giant", "Netherland Dwarf", "Angora Rabbit", "Californian Rabbit"
                ),
                aliasMap(
                        alias("Holland Lop", "holland lop", "lop"),
                        alias("Mini Rex", "mini rex", "rex"),
                        alias("Lionhead", "lionhead"),
                        alias("Angora Rabbit", "angora"),
                        alias("Netherland Dwarf", "netherland dwarf", "dwarf rabbit")
                )
        );

        registerSpecies(
                "BIRD",
                "Bird",
                Arrays.asList(
                        "bird", "birds", "oiseau", "oiseaux", "parrot", "canary", "cockatiel", "macaw", "budgie",
                        "budgerigar", "lovebird", "finch", "conure", "african grey"
                ),
                Arrays.asList(
                        "Parakeet", "Cockatiel", "Macaw", "African Grey", "Lovebird", "Budgerigar", "Canary", "Finch", "Conure", "Parrot"
                ),
                aliasMap(
                        alias("Budgerigar", "budgerigar", "budgie"),
                        alias("Cockatiel", "cockatiel"),
                        alias("Macaw", "macaw"),
                        alias("African Grey", "african grey", "grey parrot"),
                        alias("Lovebird", "lovebird"),
                        alias("Parakeet", "parakeet"),
                        alias("Canary", "canary"),
                        alias("Conure", "conure")
                )
        );

        registerSpecies(
                "HAMSTER",
                "Hamster",
                Arrays.asList(
                        "hamster", "hamsters", "syrian hamster", "dwarf hamster", "roborovski", "hammy"
                ),
                Arrays.asList(
                        "Syrian Hamster", "Dwarf Hamster", "Roborovski Hamster", "Campbell Hamster", "Chinese Hamster"
                ),
                aliasMap(
                        alias("Syrian Hamster", "syrian hamster", "syrian"),
                        alias("Dwarf Hamster", "dwarf hamster", "dwarf"),
                        alias("Roborovski Hamster", "roborovski", "robo hamster"),
                        alias("Chinese Hamster", "chinese hamster"),
                        alias("Campbell Hamster", "campbell hamster", "campbell")
                )
        );

        registerSpecies(
                "FISH",
                "Fish",
                Arrays.asList(
                        "fish", "fishes", "poisson", "poissons", "aquarium", "koi", "betta", "goldfish", "tetra",
                        "angelfish", "clownfish", "guppy", "molly", "discus"
                ),
                Arrays.asList(
                        "Koi", "Angelfish", "Goldfish", "Betta", "Clownfish", "Tetra", "Guppy", "Molly", "Discus", "Oscar", "Corydoras"
                ),
                aliasMap(
                        alias("Goldfish", "goldfish", "poisson rouge"),
                        alias("Angelfish", "angelfish"),
                        alias("Clownfish", "clownfish", "nemo"),
                        alias("Betta", "betta", "fighting fish"),
                        alias("Koi", "koi"),
                        alias("Tetra", "tetra"),
                        alias("Guppy", "guppy"),
                        alias("Discus", "discus")
                )
        );

        registerSpecies(
                "REPTILE",
                "Reptile",
                Arrays.asList(
                        "reptile", "reptiles", "snake", "python", "lizard", "gecko", "iguana", "turtle", "tortoise",
                        "dragon", "tegu", "skink", "serpent", "lezard", "tortue", "boa", "anole"
                ),
                Arrays.asList(
                        "Ball Python", "Corn Snake", "Bearded Dragon", "Blue Tongue Skink", "Tegu", "Leopard Gecko",
                        "Green Iguana", "Red Eared Slider"
                ),
                aliasMap(
                        alias("Ball Python", "ball python", "python"),
                        alias("Corn Snake", "corn snake"),
                        alias("Bearded Dragon", "bearded dragon", "beardie"),
                        alias("Blue Tongue Skink", "blue tongue skink", "blue tongue"),
                        alias("Leopard Gecko", "leopard gecko", "gecko"),
                        alias("Green Iguana", "green iguana", "iguana"),
                        alias("Red Eared Slider", "red eared slider", "slider turtle", "turtle"),
                        alias("Tegu", "tegu")
                )
        );

        registerSpecies(
                "RODENT",
                "Rodent",
                Arrays.asList(
                        "rodent", "rodents", "guinea pig", "guineapig", "chinchilla", "gerbil", "dormouse", "mouse",
                        "mice", "rat", "souris", "hamsterlike"
                ),
                Arrays.asList(
                        "Guinea Pig", "Chinchilla", "Gerbil", "Dormouse", "Mouse", "Rat", "Fancy Rat"
                ),
                aliasMap(
                        alias("Guinea Pig", "guinea pig", "guineapig", "cavy"),
                        alias("Chinchilla", "chinchilla"),
                        alias("Gerbil", "gerbil"),
                        alias("Dormouse", "dormouse"),
                        alias("Fancy Rat", "fancy rat")
                )
        );

        registerSpecies(
                "FARM",
                "Farm Animal",
                Arrays.asList(
                        "farm", "goat", "sheep", "chicken", "duck", "turkey", "pig", "donkey", "llama", "alpaca",
                        "cow", "horse", "chevre", "mouton", "poule", "canard", "coq", "pony", "mini pig"
                ),
                Arrays.asList(
                        "Goat", "Sheep", "Chicken", "Duck", "Turkey", "Mini Pig", "Donkey", "Llama", "Alpaca", "Cow", "Horse"
                ),
                aliasMap(
                        alias("Mini Pig", "mini pig", "piglet", "pet pig"),
                        alias("Goat", "goat", "chevre"),
                        alias("Sheep", "sheep", "mouton"),
                        alias("Chicken", "chicken", "poule"),
                        alias("Duck", "duck", "canard"),
                        alias("Donkey", "donkey", "ane"),
                        alias("Llama", "llama"),
                        alias("Alpaca", "alpaca"),
                        alias("Horse", "horse", "pony")
                )
        );

        registerSpecies(
                "EXOTIC",
                "Exotic Animal",
                Arrays.asList(
                        "exotic", "fennec", "sugar glider", "hedgehog", "serval", "coati", "monkey", "wallaby",
                        "skunk", "lemur", "marmoset", "raccoon", "kinkajou"
                ),
                Arrays.asList(
                        "Fennec Fox", "Sugar Glider", "Hedgehog", "Serval", "Coati", "Capuchin Monkey", "Skunk",
                        "Wallaby", "Lemur", "Marmoset", "Raccoon"
                ),
                aliasMap(
                        alias("Fennec Fox", "fennec"),
                        alias("Sugar Glider", "sugar glider"),
                        alias("Hedgehog", "hedgehog", "herisson"),
                        alias("Serval", "serval"),
                        alias("Capuchin Monkey", "capuchin", "monkey"),
                        alias("Wallaby", "wallaby"),
                        alias("Marmoset", "marmoset"),
                        alias("Lemur", "lemur")
                )
        );

        registerSpecies(
                "SMALL_MAMMAL",
                "Small Mammal",
                Arrays.asList(
                        "small mammal", "mammal", "ferret", "degu", "prairie dog", "otter", "stoat"
                ),
                Arrays.asList(
                        "Ferret", "Degu", "Prairie Dog", "Mini Hedgehog", "Otter"
                ),
                aliasMap(
                        alias("Ferret", "ferret", "furet"),
                        alias("Degu", "degu"),
                        alias("Prairie Dog", "prairie dog"),
                        alias("Mini Hedgehog", "mini hedgehog"),
                        alias("Otter", "otter")
                )
        );

        registerSpecies(
                "TURTLE",
                "Turtle",
                Arrays.asList(
                        "turtle", "tortoise", "tortue", "slider turtle", "box turtle"
                ),
                Arrays.asList(
                        "Red Eared Slider", "Russian Tortoise", "Sulcata Tortoise", "Box Turtle", "Greek Tortoise"
                ),
                aliasMap(
                        alias("Red Eared Slider", "red eared slider", "slider turtle", "turtle"),
                        alias("Russian Tortoise", "russian tortoise"),
                        alias("Sulcata Tortoise", "sulcata tortoise"),
                        alias("Box Turtle", "box turtle"),
                        alias("Greek Tortoise", "greek tortoise")
                )
        );
    }

    private static final class SpeciesProfile {
        private final String key;
        private final String displayName;
        private final Set<String> keywords = new LinkedHashSet<>();
        private final List<String> breeds = new ArrayList<>();
        private final Map<String, String> breedAliases = new LinkedHashMap<>();

        private SpeciesProfile(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        private void addKeyword(String keyword) {
            String normalized = normalize(keyword);
            if (!normalized.isBlank()) {
                keywords.add(normalized);
            }
        }

        private void addBreed(String breed) {
            if (breed == null || breed.isBlank()) {
                return;
            }

            breeds.add(breed);
            addAliasToBreed(breed, breed);

            String normalizedBreed = normalize(breed);
            for (String token : normalizedBreed.split("\\s+")) {
                if (token.length() >= 4) {
                    addAliasToBreed(token, breed);
                }
            }
        }

        private void addBreedAlias(String breed, String alias) {
            if (breed == null || breed.isBlank() || alias == null || alias.isBlank()) {
                return;
            }

            if (!breeds.contains(breed)) {
                breeds.add(breed);
            }
            addAliasToBreed(alias, breed);
        }

        private void addAliasToBreed(String alias, String breed) {
            String normalizedAlias = normalize(alias);
            if (normalizedAlias.isBlank()) {
                return;
            }

            breedAliases.putIfAbsent(normalizedAlias, breed);

            String compact = normalizedAlias.replace(" ", "");
            if (compact.length() >= 4) {
                breedAliases.putIfAbsent(compact, breed);
            }

            if (normalizedAlias.contains(" ")) {
                for (String token : normalizedAlias.split("\\s+")) {
                    if (token.length() >= 4) {
                        breedAliases.putIfAbsent(token, breed);
                    }
                }
            }
        }
    }

    private static final class AliasEntry {
        private final String breed;
        private final String[] aliases;

        private AliasEntry(String breed, String[] aliases) {
            this.breed = breed;
            this.aliases = aliases == null ? new String[0] : aliases;
        }
    }

    private static final class BreedMatch {
        private final String breed;
        private final int aliasScore;

        private BreedMatch(String breed, int aliasScore) {
            this.breed = breed;
            this.aliasScore = aliasScore;
        }
    }

    private static final class MatchResult {
        private final SpeciesProfile profile;
        private final String detectedBreed;
        private final int keywordHits;
        private final int bestBreedAliasScore;
        private final int totalScore;

        private MatchResult(SpeciesProfile profile, String detectedBreed, int keywordHits, int bestBreedAliasScore) {
            this.profile = profile;
            this.detectedBreed = detectedBreed;
            this.keywordHits = keywordHits;
            this.bestBreedAliasScore = bestBreedAliasScore;
            this.totalScore = (detectedBreed != null ? 1000 : 0) + (keywordHits * 20) + bestBreedAliasScore;
        }

        private boolean isBetterThan(MatchResult other) {
            if (totalScore != other.totalScore) {
                return totalScore > other.totalScore;
            }

            int thisBreedLen = detectedBreed == null ? -1 : detectedBreed.length();
            int otherBreedLen = other.detectedBreed == null ? -1 : other.detectedBreed.length();
            if (thisBreedLen != otherBreedLen) {
                return thisBreedLen > otherBreedLen;
            }

            return profile.key.compareTo(other.profile.key) < 0;
        }
    }

    public static class AnimalRecognitionResult {
        private String species;
        private String breed;
        private float confidence;
        private String imagePath;

        public String getSpecies() {
            return species;
        }

        public void setSpecies(String species) {
            this.species = species;
        }

        public String getBreed() {
            return breed;
        }

        public void setBreed(String breed) {
            this.breed = breed;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        @Override
        public String toString() {
            return String.format("Species: %s | Breed: %s | Confidence: %.0f%%", species, breed, confidence * 100);
        }
    }
}

