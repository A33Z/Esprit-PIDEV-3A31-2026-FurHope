package com.esprit.controllers;

import com.esprit.entities.animal;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnimalCard {

    @FXML private ImageView animalImage;
    @FXML private Label nameLabel;
    @FXML private Label speciesLabel;
    @FXML private Label ageLabel;
    @FXML private Label statusLabel;
    @FXML private Button favoriteButton;

    private animal currentAnimal;
    private boolean isFavorite = false;
    private static final List<animal> favoriteAnimals = new ArrayList<>();
    private Runnable refreshCallback; // ✅ Simple Runnable

    /**
     * ✅ Définir le callback pour rafraîchir
     */
    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    /**
     * ✅ Remplir la card avec les données de l'animal
     */
    public void setData(animal animal) {
        this.currentAnimal = animal;

        nameLabel.setText("🐾 " + animal.getName());
        speciesLabel.setText("Espèce: " + animal.getSpecies());
        ageLabel.setText("Âge: " + animal.getAge() + " ans");

        updateStatusLabel();
        loadAnimalImage(animal);
        updateFavoriteButton();
    }

    /**
     * 📌 Mettre à jour le label de statut
     */
    private void updateStatusLabel() {
        if (currentAnimal == null) return;

        String statusText = currentAnimal.getStatus().toString();
        statusLabel.setText(statusText);

        if (statusText.equals("AVAILABLE")) {
            statusLabel.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; " +
                            "-fx-text-fill: #27ae60; " +
                            "-fx-padding: 5 12; " +
                            "-fx-background-color: #d5f4e6; " +
                            "-fx-border-radius: 15; " +
                            "-fx-background-radius: 15;"
            );
        } else if (statusText.equals("ADOPTED")) {
            statusLabel.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; " +
                            "-fx-text-fill: #ffffff; " +
                            "-fx-padding: 5 12; " +
                            "-fx-background-color: #e74c3c; " +
                            "-fx-border-radius: 15; " +
                            "-fx-background-radius: 15;"
            );
        } else {
            statusLabel.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; " +
                            "-fx-text-fill: #f39c12; " +
                            "-fx-padding: 5 12; " +
                            "-fx-background-color: #fef5e7; " +
                            "-fx-border-radius: 15; " +
                            "-fx-background-radius: 15;"
            );
        }
    }

    /**
     * 🖼️ Charger l'image de l'animal
     */
    private void loadAnimalImage(animal animal) {
        try {
            if (animal.getImage() != null && !animal.getImage().isEmpty()) {
                File imageFile = new File("images/" + animal.getImage());
                if (imageFile.exists()) {
                    animalImage.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    setDefaultImage();
                }
            } else {
                setDefaultImage();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement image: " + e.getMessage());
            e.printStackTrace();
            setDefaultImage();
        }
    }

    /**
     * 📷 Image par défaut
     */
    private void setDefaultImage() {
        animalImage.setStyle("-fx-text-fill: #bdc3c7;");
    }

    /**
     * ❤️ Ajouter/Retirer des favoris
     */
    @FXML
    private void handleFavoriteButton() {
        if (currentAnimal == null) return;

        isFavorite = !isFavorite;

        if (isFavorite) {
            // Ajouter aux favoris
            if (!favoriteAnimals.contains(currentAnimal)) {
                favoriteAnimals.add(currentAnimal);
            }
            favoriteButton.setText("❤️");
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip != null) {
                tooltip.setText("Retirer des favoris");
            } else {
                favoriteButton.setTooltip(new Tooltip("Retirer des favoris"));
            }
            System.out.println("❤️ Ajouté aux favoris: " + currentAnimal.getName());

        } else {
            // Retirer des favoris
            favoriteAnimals.remove(currentAnimal);
            favoriteButton.setText("🤍");
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip != null) {
                tooltip.setText("Ajouter aux favoris");
            } else {
                favoriteButton.setTooltip(new Tooltip("Ajouter aux favoris"));
            }
            System.out.println("🤍 Retiré des favoris: " + currentAnimal.getName());

            // ✅ Appeler le callback si défini (pour rafraîchir la page favoris)
            if (refreshCallback != null) {
                refreshCallback.run();
            }
        }
    }

    /**
     * 🔄 Mettre à jour l'état du bouton favori
     */
    private void updateFavoriteButton() {
        if (currentAnimal == null) return;

        isFavorite = favoriteAnimals.contains(currentAnimal);

        if (isFavorite) {
            favoriteButton.setText("❤️");
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip == null) {
                favoriteButton.setTooltip(new Tooltip("Retirer des favoris"));
            } else {
                tooltip.setText("Retirer des favoris");
            }
        } else {
            favoriteButton.setText("🤍");
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip == null) {
                favoriteButton.setTooltip(new Tooltip("Ajouter aux favoris"));
            } else {
                tooltip.setText("Ajouter aux favoris");
            }
        }
    }

    /**
     * ⭐ Getters pour la gestion des favoris
     */
    public static List<animal> getFavoriteAnimals() {
        return favoriteAnimals;
    }

    public static void addFavorite(animal a) {
        if (!favoriteAnimals.contains(a)) {
            favoriteAnimals.add(a);
            System.out.println("❤️ Favori ajouté: " + a.getName());
        }
    }

    public static void removeFavorite(animal a) {
        favoriteAnimals.remove(a);
        System.out.println("🤍 Favori retiré: " + a.getName());
    }

    public static void clearFavorites() {
        favoriteAnimals.clear();
        System.out.println("🗑️ Tous les favoris supprimés");
    }
}