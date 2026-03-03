package com.esprit.Services;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Service de reconnaissance automatique des animaux via l'analyse du nom de fichier image
 * (Simulation - peut être amélioré avec une vraie API ML à l'avenir)
 */
public class AutoRecognitionService {

    private static final Map<String, AnimalSuggestion> SPECIES_DATABASE = new HashMap<>();
    private static final Random random = new Random();

    static {
        // ==================== CHIENS ====================
        SPECIES_DATABASE.put("DOG", new AnimalSuggestion("Chien",
                new String[]{
                        // Petits chiens
                        "Chihuahua", "Pomerania", "Carlin", "Shih Tzu", "Cavalier King Charles",
                        // Chiens moyens
                        "Beagle", "Cocker Spaniel", "Schnauzer", "Bulldog Français", "Jack Russell",
                        // Grands chiens
                        "Labrador Retriever", "Berger Allemand", "Golden Retriever", "Boxer",
                        "Rottweiler", "Dalmatien", "Doberman", "Saint-Bernard", "Husky Sibérien",
                        // Chiens spécialisés
                        "Poodle", "Dachshund", "Terrier", "Colley", "Setter", "Pointer",
                        "Bouvier des Flandres", "Berger Belge", "Shar-Pei"
                }));

        // ==================== CHATS ====================
        SPECIES_DATABASE.put("CAT", new AnimalSuggestion("Chat",
                new String[]{
                        // Chats domestiques
                        "Persan", "Siamois", "Maine Coon", "Bengal", "Angora",
                        "Sacré de Birmanie", "Ragdoll", "Abyssin", "Bombay", "Burmese",
                        // Chats rares
                        "Sphynx", "Cornish Rex", "Devon Rex", "Manx", "Korat",
                        "Tonkinois", "Balinais", "Somali", "Chartreux", "Bleu Russe",
                        // Chats européens
                        "Chat Européen", "Chat Tabby", "Chat Roux", "Chat Noir", "Chat Blanc",
                        "Calico", "Tortoiseshell"
                }));

        // ==================== LAPINS ====================
        SPECIES_DATABASE.put("RABBIT", new AnimalSuggestion("Lapin",
                new String[]{
                        "Lapin Nain", "Lapin Bélier", "Lapin Angora", "Lapin Rex",
                        "Lapin Hollandais", "Lapin Lion", "Lapin Géant", "Lapin Français",
                        "Lapin Lièvre Belge", "Lapin Papillon", "Lapin Gris Bleu", "Lapin Blanc",
                        "Lapin Chinchilla", "Lapin Himalaya", "Lapin Argent", "Lapin Fauve",
                        "Lapin Normand", "Lapin Flamand Géant"
                }));

        // ==================== OISEAUX ====================
        SPECIES_DATABASE.put("BIRD", new AnimalSuggestion("Oiseau",
                new String[]{
                        // Perroquets
                        "Perruche Ondulée", "Inséparable", "Canari", "Perroquet Gris",
                        "Ara Macaw", "Cacatoès", "Conure", "Amazone",
                        // Petits oiseaux
                        "Moineau", "Pinson", "Rossignol", "Merle", "Corbeau",
                        "Pigeon", "Tourterelle", "Colombe", "Hibou", "Faucon",
                        // Oiseaux exotiques
                        "Lori Arc-en-ciel", "Eclectus", "Kakapo", "Perruche à crinière"
                }));

        // ==================== HAMSTERS ====================
        SPECIES_DATABASE.put("HAMSTER", new AnimalSuggestion("Hamster",
                new String[]{
                        "Hamster Syrien", "Hamster Russe", "Hamster Roborovski",
                        "Hamster de Chine", "Hamster Nain", "Hamster Blanc",
                        "Hamster Gris", "Hamster Brun", "Hamster Roux"
                }));

        // ==================== POISSONS ====================
        SPECIES_DATABASE.put("FISH", new AnimalSuggestion("Poisson",
                new String[]{
                        "Poisson Rouge", "Néon Tétra", "Guppy", "Molly", "Platy",
                        "Discus", "Corydoras", "Scalaire", "Arowana", "Betta",
                        "Danio Léopard", "Poisson Chat", "Rasbora", "Haplochromis", "Tilapia"
                }));

        // ==================== REPTILES ====================
        SPECIES_DATABASE.put("REPTILE", new AnimalSuggestion("Reptile",
                new String[]{
                        "Python Royal", "Boa Constrictor", "Lézard des sables", "Caméléon",
                        "Gecko Léopard", "Iguane Vert", "Tortue Terrestre", "Tortue Aquatique",
                        "Serpent de Maïs", "Serpent des Blés", "Lézard Barbu", "Moniteur du Nil"
                }));

        // ==================== RONGEURS AUTRES ====================
        SPECIES_DATABASE.put("RODENT", new AnimalSuggestion("Rongeur",
                new String[]{
                        "Souris", "Rat", "Chinchilla", "Cochon d'Inde", "Gerbille",
                        "Écureuil", "Tamias", "Aulacode", "Capybara", "Ragondin"
                }));

        // ==================== AUTRES ====================
        SPECIES_DATABASE.put("OTHER", new AnimalSuggestion("Animal",
                new String[]{
                        "Chèvre", "Mouton", "Âne", "Lama", "Alpaca",
                        "Porc miniature", "Poule", "Canard", "Oie", "Paon"
                }));
    }

    /**
     * Analyse le nom du fichier image et retourne une suggestion
     */
    public static AnimalRecognitionResult analyzeImage(File imageFile) {
        String filename = imageFile.getName().toLowerCase();
        AnimalRecognitionResult result = new AnimalRecognitionResult();

        // Vérifier les mots-clés pour chaque espèce
        if (filename.contains("dog") || filename.contains("chien") || filename.contains("canin")) {
            fillSuggestion(result, "DOG");
        } else if (filename.contains("cat") || filename.contains("chat") || filename.contains("félin")) {
            fillSuggestion(result, "CAT");
        } else if (filename.contains("rabbit") || filename.contains("lapin") || filename.contains("bunny")) {
            fillSuggestion(result, "RABBIT");
        } else if (filename.contains("bird") || filename.contains("oiseau") || filename.contains("perroquet") || filename.contains("perruche")) {
            fillSuggestion(result, "BIRD");
        } else if (filename.contains("hamster") || filename.contains("rongeur")) {
            fillSuggestion(result, "HAMSTER");
        } else if (filename.contains("fish") || filename.contains("poisson")) {
            fillSuggestion(result, "FISH");
        } else if (filename.contains("reptile") || filename.contains("serpent") || filename.contains("lézard") || filename.contains("tortue")) {
            fillSuggestion(result, "REPTILE");
        } else if (filename.contains("rodent") || filename.contains("souris") || filename.contains("rat")) {
            fillSuggestion(result, "RODENT");
        } else if (filename.contains("other") || filename.contains("autre") || filename.contains("animal")) {
            fillSuggestion(result, "OTHER");
        } else {
            // Par défaut - confiance basse
            result.setSpecies("À déterminer");
            result.setBreed("À déterminer");
            result.setConfidence(0.30f);
        }

        result.setImagePath(imageFile.getAbsolutePath());
        return result;
    }

    /**
     * Remplit la suggestion avec les données de la base
     */
    private static void fillSuggestion(AnimalRecognitionResult result, String speciesKey) {
        AnimalSuggestion suggestion = SPECIES_DATABASE.get(speciesKey);
        if (suggestion != null) {
            result.setSpecies(suggestion.species);
            result.setBreed(getRandomBreed(speciesKey));
            result.setConfidence(0.85f + (random.nextFloat() * 0.15f)); // 0.85 à 1.0
        }
    }

    /**
     * Retourne une race aléatoire pour une espèce
     */
    private static String getRandomBreed(String species) {
        AnimalSuggestion suggestion = SPECIES_DATABASE.get(species);
        if (suggestion != null && suggestion.breeds.length > 0) {
            return suggestion.breeds[random.nextInt(suggestion.breeds.length)];
        }
        return "Race inconnue";
    }

    /**
     * Classe interne pour stocker les suggestions de races
     */
    private static class AnimalSuggestion {
        String species;
        String[] breeds;

        AnimalSuggestion(String species, String[] breeds) {
            this.species = species;
            this.breeds = breeds;
        }
    }

    /**
     * Classe pour les résultats de reconnaissance
     */
    public static class AnimalRecognitionResult {
        private String species;
        private String breed;
        private float confidence;
        private String imagePath;

        // Getters & Setters
        public String getSpecies() { return species; }
        public void setSpecies(String species) { this.species = species; }

        public String getBreed() { return breed; }
        public void setBreed(String breed) { this.breed = breed; }

        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }

        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }

        @Override
        public String toString() {
            return String.format("Espèce: %s | Race: %s | Confiance: %.0f%%",
                    species, breed, confidence * 100);
        }
    }
}