package com.esprit.controllers;

import com.esprit.entities.animal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class favoritecontroller {

    @FXML
    private GridPane favoritesGrid;

    @FXML
    private Label emptyLabel;

    @FXML
    private Button backButton;

    @FXML
    public void initialize() {
        loadFavorites();
    }

    /**
     * 🔄 Charger et afficher les favoris
     */
    public void loadFavorites() {
        favoritesGrid.getChildren().clear();

        // Récupérer la liste statique des favoris
        List<animal> favorites = AnimalCard.getFavoriteAnimals();
        System.out.println("📋 Nombre de favoris: " + favorites.size());

        // Si aucun favori
        if (favorites.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setText("❤️ Aucun animal favori pour le moment.\n\nRetournez à la liste pour en ajouter!");
            return;
        }

        emptyLabel.setVisible(false);

        // Afficher les favoris en grille (2 colonnes)
        int col = 0;
        int row = 0;

        for (animal a : favorites) {
            System.out.println("❤️ Affichage favori: " + a.getName());

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AnimalCard.fxml"));
                AnchorPane card = loader.load();

                AnimalCard controller = loader.getController();
                controller.setData(a);

                // ✅ Passer le callback pour rafraîchir quand on retire un favori
                controller.setRefreshCallback(() -> this.refreshFavorites());

                favoritesGrid.add(card, col, row);

                col++;
                if (col == 2) { // 2 colonnes
                    col = 0;
                    row++;
                }

            } catch (IOException e) {
                System.err.println("❌ Erreur chargement card: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * ⬅️ Retourner à la liste des animaux
     */
    @FXML
    void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherAnimal.fxml"));

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            System.out.println("⬅️ Retour à la liste des animaux");

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🔄 Rafraîchir la liste (appelé quand on retire un favori)
     */
    public void refreshFavorites() {
        System.out.println("🔄 Rafraîchissement des favoris");
        loadFavorites();
    }

    /**
     * 🗑️ Vider tous les favoris
     */
    @FXML
    void clearAllFavorites() {
        List<animal> favorites = AnimalCard.getFavoriteAnimals();

        if (favorites.isEmpty()) {
            showAlert("ℹ️ Info", "Il n'y a aucun favori à supprimer", Alert.AlertType.INFORMATION);
            return;
        }

        // Confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("🗑️ Confirmation");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer TOUS les favoris?");

        if (confirmAlert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            AnimalCard.clearFavorites();
            System.out.println("🗑️ Tous les favoris supprimés");
            loadFavorites();
            showAlert("✓ Succès", "Tous les favoris ont été supprimés", Alert.AlertType.INFORMATION);
        }
    }

    /**
     * 📊 Afficher les statistiques des favoris
     */
    @FXML
    void showStats() {
        List<animal> favorites = AnimalCard.getFavoriteAnimals();

        if (favorites.isEmpty()) {
            showAlert("ℹ️ Info", "Aucun favori", Alert.AlertType.INFORMATION);
            return;
        }

        StringBuilder stats = new StringBuilder();
        stats.append("📊 STATISTIQUES FAVORIS\n\n");
        stats.append("Total d'animaux favoris: ").append(favorites.size()).append("\n\n");
        stats.append("Par espèce:\n");

        // Compter par espèce
        java.util.Map<String, Integer> specieCount = new java.util.HashMap<>();
        for (animal a : favorites) {
            specieCount.put(a.getSpecies(), specieCount.getOrDefault(a.getSpecies(), 0) + 1);
        }

        // Afficher les stats
        for (String species : specieCount.keySet()) {
            stats.append("  • ").append(species).append(": ").append(specieCount.get(species)).append("\n");
        }

        System.out.println(stats);
        showAlert("📊 Statistiques", stats.toString(), Alert.AlertType.INFORMATION);
    }

    /**
     * 🎨 Afficher un alert
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}