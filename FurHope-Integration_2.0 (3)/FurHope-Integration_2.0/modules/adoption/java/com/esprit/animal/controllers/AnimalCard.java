package com.esprit.animal.controllers;

import com.esprit.animal.entities.animal;
import com.esprit.animal.i18n.LanguageManager;
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
    private static final String HEART_EMPTY = "\u2661";
    private static final String HEART_FILLED = "\u2665";
    private static final String HEART_STYLE_BASE =
            "-fx-font-size: 24px; -fx-background-color: transparent; -fx-padding: 0; " +
                    "-fx-focus-traversable: false; -fx-cursor: hand;";

    @FXML
    private ImageView animalImage;
    @FXML
    private Label nameLabel;
    @FXML
    private Label speciesLabel;
    @FXML
    private Label ageLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button favoriteButton;

    private animal currentAnimal;
    private boolean isFavorite = false;
    private static final List<animal> favoriteAnimals = new ArrayList<>();
    private Runnable refreshCallback;

    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    public void setData(animal animal) {
        this.currentAnimal = animal;

        nameLabel.setText(animal.getName());
        speciesLabel.setText(tr("animal.card.species") + ": " + animal.getSpecies());
        ageLabel.setText(tr("animal.card.age") + ": " + animal.getAge() + " " + tr("animal.card.years"));

        updateStatusLabel();
        loadAnimalImage(animal);
        updateFavoriteButton();
    }

    private void updateStatusLabel() {
        if (currentAnimal == null) {
            return;
        }

        String statusText = currentAnimal.getStatus().toString();
        String localizedStatus = switch (currentAnimal.getStatus()) {
            case AVAILABLE -> tr("status.available");
            case ADOPTED -> tr("status.adopted");
            case UNAVAILABLE -> tr("status.unavailable");
        };
        statusLabel.setText(localizedStatus);

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
            System.err.println("Error loading image: " + e.getMessage());
            e.printStackTrace();
            setDefaultImage();
        }
    }

    private void setDefaultImage() {
        animalImage.setStyle("-fx-text-fill: #bdc3c7;");
    }

    @FXML
    private void handleFavoriteButton() {
        if (currentAnimal == null) {
            return;
        }

        isFavorite = !isFavorite;

        if (isFavorite) {
            if (!favoriteAnimals.contains(currentAnimal)) {
                favoriteAnimals.add(currentAnimal);
            }
            applyFavoriteVisual(true);
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip != null) {
                tooltip.setText(tr("animal.card.favoriteRemove"));
            } else {
                favoriteButton.setTooltip(new Tooltip(tr("animal.card.favoriteRemove")));
            }
            System.out.println("Added to favorites: " + currentAnimal.getName());

        } else {
            favoriteAnimals.remove(currentAnimal);
            applyFavoriteVisual(false);
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip != null) {
                tooltip.setText(tr("animal.card.favoriteAdd"));
            } else {
                favoriteButton.setTooltip(new Tooltip(tr("animal.card.favoriteAdd")));
            }
            System.out.println("Removed from favorites: " + currentAnimal.getName());

            if (refreshCallback != null) {
                refreshCallback.run();
            }
        }
    }

    private void updateFavoriteButton() {
        if (currentAnimal == null) {
            return;
        }

        isFavorite = favoriteAnimals.contains(currentAnimal);

        if (isFavorite) {
            applyFavoriteVisual(true);
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip == null) {
                favoriteButton.setTooltip(new Tooltip(tr("animal.card.favoriteRemove")));
            } else {
                tooltip.setText(tr("animal.card.favoriteRemove"));
            }
        } else {
            applyFavoriteVisual(false);
            Tooltip tooltip = favoriteButton.getTooltip();
            if (tooltip == null) {
                favoriteButton.setTooltip(new Tooltip(tr("animal.card.favoriteAdd")));
            } else {
                tooltip.setText(tr("animal.card.favoriteAdd"));
            }
        }
    }

    public static List<animal> getFavoriteAnimals() {
        return favoriteAnimals;
    }

    public static void addFavorite(animal a) {
        if (!favoriteAnimals.contains(a)) {
            favoriteAnimals.add(a);
            System.out.println("Favorite added: " + a.getName());
        }
    }

    public static void removeFavorite(animal a) {
        favoriteAnimals.remove(a);
        System.out.println("Favorite removed: " + a.getName());
    }

    public static void clearFavorites() {
        favoriteAnimals.clear();
        System.out.println("All favorites deleted");
    }

    private void applyFavoriteVisual(boolean favorite) {
        if (favorite) {
            favoriteButton.setText(HEART_FILLED);
            favoriteButton.setStyle(HEART_STYLE_BASE + " -fx-text-fill: #e11d48;");
        } else {
            favoriteButton.setText(HEART_EMPTY);
            favoriteButton.setStyle(HEART_STYLE_BASE + " -fx-text-fill: #94a3b8;");
        }
    }

    private String tr(String key) {
        try {
            return LanguageManager.get(key);
        } catch (Exception e) {
            return key;
        }
    }
}

