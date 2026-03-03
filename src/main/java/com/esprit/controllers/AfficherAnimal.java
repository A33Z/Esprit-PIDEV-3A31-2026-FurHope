package com.esprit.controllers;

import com.esprit.utils.Session;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.esprit.Services.animalServices;
import com.esprit.entities.animal;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AfficherAnimal {

    @FXML private ListView<animal> listview;
    @FXML private Button btnAddAnimal;
    @FXML private Button btnRequests;
    @FXML private Button btnFavorite;
    @FXML private Button btnMyRequests;
    @FXML private Button btnMyAnimals;
    @FXML private Label statusLabel;

    private animalServices ps = new animalServices();

    @FXML
    public void initialize() {
        // Configurer le rendu des cellules
        listview.setCellFactory(param -> new ListCell<animal>() {
            @Override
            protected void updateItem(animal a, boolean empty) {
                super.updateItem(a, empty);

                if (empty || a == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AnimalCard.fxml"));
                        AnchorPane pane = loader.load();

                        AnimalCard controller = loader.getController();
                        controller.setData(a);

                        setGraphic(pane);
                        setPrefWidth(Control.USE_COMPUTED_SIZE);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Double-clic pour voir les détails
        listview.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                animal selectedAnimal = listview.getSelectionModel().getSelectedItem();
                if (selectedAnimal != null) {
                    openAnimalDetails(selectedAnimal);
                }
            }
        });

        // Charger les données
        refreshAnimalList();

        // Actions des boutons
        btnAddAnimal.setOnAction(e -> navigateToAddAnimal());
    }

    /**
     * 🔄 Actualiser la liste des animaux
     */
    private void refreshAnimalList() {
        try {
            List<animal> animals = ps.afficher();
            ObservableList<animal> observableList = FXCollections.observableList(animals);
            listview.setItems(observableList);

            statusLabel.setText("✓ " + observableList.size() + " animaux trouvés");
        } catch (SQLException e) {
            showAlert("Erreur lors du chargement", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * 🐾 Ouvrir les détails d'un animal
     */
    private void openAnimalDetails(animal selectedAnimal) {
        try {
            FXMLLoader loader;
            Parent root;

            // Vérifier si l'utilisateur connecté est le propriétaire
            if (selectedAnimal.getOwnerid() == com.esprit.utils.Session.getUserId()) {
                // Vue propriétaire (avec modifier/supprimer)
                loader = new FXMLLoader(getClass().getResource("/AnimalDetails.fxml"));
                root = loader.load();

                AnimalDetails controller = loader.getController();
                controller.setAnimal(selectedAnimal);

            } else {
                // Vue adoptant (avec demande d'adoption)
                loader = new FXMLLoader(getClass().getResource("/adopanimaldetails.fxml"));
                root = loader.load();

                adopdetails controller = loader.getController();
                controller.setPetData(selectedAnimal);
            }

            Stage stage = new Stage();
            stage.setTitle("🐾 Détails de l'animal");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            Stage currentStage = (Stage) listview.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de l'ouverture des détails", Alert.AlertType.ERROR);
        }
    }

    /**
     * ➕ Naviguer vers l'ajout d'animal
     */
    private void navigateToAddAnimal() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterAnimal.fxml"));
            btnAddAnimal.getScene().setRoot(root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * ❤️ Ouvrir la liste des favoris
     */
    @FXML
    private void openFavoriteList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/favoriteAnimal.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("❤️ Mes animaux favoris");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 📧 Voir les demandes reçues pour mes animaux
     */
    @FXML
    void openRequests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Requests.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("📧 Demandes pour mes animaux");
            stage.setMaximized(true);
            stage.show();

            Stage currentStage = (Stage) listview.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 📋 Voir mes demandes d'adoption envoyées
     */
    @FXML
    void openMessageRequests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRequest.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("📋 Mes demandes d'adoption");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            Stage currentStage = (Stage) listview.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🏠 Voir mes animaux
     */
    @FXML
    void openMyAnimals(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MyAnimals.fxml"));
            listview.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    void logout(ActionEvent event) {
        // Vider la session
        Session.logout();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) listview.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
            stage.setTitle("🔐 Connexion");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur lors du logout", Alert.AlertType.ERROR);
        }
    }

    public void removeAnimalFromList(animal animal) {
        listview.getItems().remove(animal);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type.toString());
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}