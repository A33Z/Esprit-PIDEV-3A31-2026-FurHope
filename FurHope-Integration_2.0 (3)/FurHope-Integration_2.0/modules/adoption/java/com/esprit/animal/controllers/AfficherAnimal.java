package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.Services.animalServices;
import com.esprit.animal.entities.animal;
import com.esprit.animal.i18n.LanguageManager;
import com.esprit.animal.utils.Session;
import controllers.SessionContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import utils.SessionManager;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class AfficherAnimal extends BaseUIController {

    @FXML
    private ListView<animal> listview;
    @FXML
    private Button btnAddAnimal;
    @FXML
    private Button btnRequests;
    @FXML
    private Button btnFavorite;
    @FXML
    private Button btnMyRequests;
    @FXML
    private Button btnMyAnimals;
    @FXML
    private Label statusLabel;

    private final animalServices ps = new animalServices();

    @Override
    protected String getViewPath() {
        return "/animal/AfficherAnimal.fxml";
    }

    @Override
    protected String getBackViewPath() {
        return "/animal/Home.fxml";
    }

    @FXML
    public void initialize() {
        listview.setCellFactory(param -> new ListCell<animal>() {
            @Override
            protected void updateItem(animal a, boolean empty) {
                super.updateItem(a, empty);

                if (empty || a == null) {
                    setGraphic(null);
                } else {
                    try {
                        FXMLLoader loader = createLoader("/animal/AnimalCard.fxml");
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

        listview.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                animal selectedAnimal = listview.getSelectionModel().getSelectedItem();
                if (selectedAnimal != null) {
                    openAnimalDetails(selectedAnimal);
                }
            }
        });

        refreshAnimalList();
        btnAddAnimal.setOnAction(e -> navigateToAddAnimal());
    }

    private void refreshAnimalList() {
        try {
            List<animal> animals = ps.afficher();
            int currentCompteId = Session.getCompteId();
            int currentUserId = Session.getUserId();
            animals.sort(
                    Comparator.comparing((animal a) ->
                                    a.getOwnerCompteId() == currentCompteId || a.getOwnerCompteId() == currentUserId)
                            .thenComparing(Comparator.comparingInt(animal::getId).reversed())
            );
            ObservableList<animal> observableList = FXCollections.observableList(animals);
            listview.setItems(observableList);
            statusLabel.setText(tr("animals.loaded") + ": " + observableList.size());
        } catch (Exception e) {
            showAlert(tr("animals.error.loading"), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void openAnimalDetails(animal selectedAnimal) {
        try {
            FXMLLoader loader;
            Parent root;
            int currentCompteId = Session.getCompteId();
            int currentUserId = Session.getUserId();
            boolean isOwner = selectedAnimal.getOwnerCompteId() == currentCompteId
                    || selectedAnimal.getOwnerCompteId() == currentUserId;

            if (isOwner) {
                loader = createLoader("/animal/animalDetails.fxml");
                root = loader.load();

                AnimalDetails controller = loader.getController();
                controller.setAnimal(selectedAnimal);
            } else {
                loader = createLoader("/animal/adopanimaldetails.fxml");
                root = loader.load();

                adopdetails controller = loader.getController();
                controller.setPetData(selectedAnimal);
            }

            Stage stage = new Stage();
            stage.setTitle("Animal Details");
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.show();

            Stage currentStage = (Stage) listview.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(tr("animals.error.openDetails"), Alert.AlertType.ERROR);
        }
    }

    private void navigateToAddAnimal() {
        try {
            Parent root = loadView("/animal/AjouterAnimal.fxml");
            btnAddAnimal.getScene().setRoot(root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void openFavoriteList() {
        try {
            FXMLLoader loader = createLoader("/animal/favoriteAnimal.fxml");
            Parent root = loader.load();

            Stage stage = new Stage();
            StageSceneHelper.setScene(stage, root);
            stage.setTitle("My Favorite Animals");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openRequests(ActionEvent event) {
        try {
            FXMLLoader loader = createLoader("/animal/Requests.fxml");
            Parent root = loader.load();

            Stage stage = new Stage();
            StageSceneHelper.setScene(stage, root);
            stage.setTitle("Requests For My Animals");
            stage.setMaximized(true);
            stage.show();

            Stage currentStage = (Stage) listview.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openMessageRequests(ActionEvent event) {
        try {
            FXMLLoader loader = createLoader("/animal/AfficherRequest.fxml");
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("My Adoption Requests");
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.show();

            Stage currentStage = (Stage) listview.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openMyAnimals(ActionEvent event) {
        try {
            Parent root = loadView("/animal/MyAnimals.fxml");
            listview.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void logout(ActionEvent event) {
        Session.logout();
        SessionContext.clear();
        SessionManager.logout();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/accueil.fxml"));
            Stage stage = (Stage) listview.getScene().getWindow();
            StageSceneHelper.setScene(stage, root);
            stage.setMaximized(true);
            stage.setTitle("FurHope");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(tr("auth.error.logout"), Alert.AlertType.ERROR);
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

    private String tr(String key) {
        try {
            return LanguageManager.get(key);
        } catch (Exception e) {
            return key;
        }
    }
}



