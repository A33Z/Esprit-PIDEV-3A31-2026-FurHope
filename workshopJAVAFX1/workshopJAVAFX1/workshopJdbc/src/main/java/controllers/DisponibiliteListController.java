package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.Disponibilite;
import services.ServiceDisponibilite;
import utils.EditState;
import utils.ViewNavigator;

import java.sql.SQLException;

public class DisponibiliteListController {

    @FXML
    private ListView<Disponibilite> disponibiliteList;
    @FXML
    private Label detailsLabel;

    private final ServiceDisponibilite service = new ServiceDisponibilite();

    @FXML
    public void initialize() {
        // ✅ Design moderne, sans afficher les IDs
        disponibiliteList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Disponibilite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox();
                    box.setSpacing(4);
                    box.setStyle("-fx-background-color: #ffffff; "
                            + "-fx-background-radius: 10; "
                            + "-fx-padding: 10;");



                    // Ligne 2 : horaire et statut
                    Label horaireLabel = new Label("🕒 " + item.getStarttime() + " → " + item.getEndtime()
                            + "   •   Statut : " + item.getStatut().name().toLowerCase());
                    horaireLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #166534;");

                    box.getChildren().addAll( horaireLabel);

                    // Ombre esthétique
                    box.setEffect(new DropShadow(2, Color.LIGHTGREEN));

                    setGraphic(box);
                }
            }
        });

        // ✅ Texte de détail simplifié et sans ID
        disponibiliteList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                detailsLabel.setText("Sélectionnez une disponibilité pour voir les détails, modifier ou supprimer.");
            } else {
                detailsLabel.setText(
                                "🕒 De " + selected.getStarttime() + " à " + selected.getEndtime() + "\n" +
                                "📅 Statut : " + selected.getStatut().name().toLowerCase()
                );
            }
        });

        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onAdd(javafx.event.ActionEvent event) {
        EditState.disponibiliteToEdit = null;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Disponibilite - Formulaire");
    }

    @FXML
    private void onEdit(javafx.event.ActionEvent event) {
        Disponibilite selected = disponibiliteList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Choisissez une disponibilité à modifier.");
            return;
        }
        EditState.disponibiliteToEdit = selected;
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Disponibilite - Modification");
    }

    @FXML
    private void onDelete() {
        Disponibilite selected = disponibiliteList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Choisissez une disponibilité à supprimer.");
            return;
        }

        try {
            service.delete(selected.getId_disponibilite());
            showInfo("Disponibilité supprimée.");
            refresh();
        } catch (SQLException e) {
            showError("Suppression impossible : " + e.getMessage());
        }
    }

    @FXML
    private void onBackHome(javafx.event.ActionEvent event) {
        ViewNavigator.goTo(event, "/Home.fxml", "Gestion Vétérinaire");
    }

    private void refresh() {
        try {
            disponibiliteList.setItems(FXCollections.observableArrayList(service.read()));
            detailsLabel.setText("Sélectionnez une disponibilité pour voir les détails, modifier ou supprimer.");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Affichage disponibilités");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}