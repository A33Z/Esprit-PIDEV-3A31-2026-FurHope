package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Rendezvous;
import services.ServiceDisponibilite;
import services.ServiceRendezvous;
import utils.SessionManager;
import utils.ViewNavigator;

import java.sql.SQLException;
import java.util.List;

public class MesRendezvousController {

    @FXML private VBox cardsContainer;

    private final ServiceRendezvous serviceRdv = new ServiceRendezvous();
    private final ServiceDisponibilite serviceDispo = new ServiceDisponibilite();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        cardsContainer.getChildren().clear();
        try {
            List<Rendezvous> list = serviceRdv.readByClientId(SessionManager.getUserId());

            if (list.isEmpty()) {
                Label empty = new Label("Aucun rendez-vous trouvé.");
                empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 15;");
                cardsContainer.getChildren().add(empty);
                return;
            }

            for (Rendezvous rdv : list) {
                cardsContainer.getChildren().add(buildCard(rdv));
            }

        } catch (SQLException e) {
            Label err = new Label("❌ Erreur : " + e.getMessage());
            err.setStyle("-fx-text-fill: #dc2626;");
            cardsContainer.getChildren().add(err);
        }
    }

    private VBox buildCard(Rendezvous rdv) {

        String borderColor, badgeBg, badgeText, statusLabel;
        switch (rdv.getStatus()) {
            case "CONFIRME" -> {
                borderColor = "#22c55e";
                badgeBg = "#dcfce7";
                badgeText = "#16a34a";
                statusLabel = "✅ Confirmé";
            }
            case "ANNULE" -> {
                borderColor = "#ef4444";
                badgeBg = "#fee2e2";
                badgeText = "#dc2626";
                statusLabel = "❌ Annulé";
            }
            default -> {
                borderColor = "#f59e0b";
                badgeBg = "#fef3c7";
                badgeText = "#d97706";
                statusLabel = "⏳ En attente";
            }
        }


        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 2;" +
                        "-fx-padding: 18;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);"
        );

        HBox topRow = new HBox(12);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label badge = new Label(statusLabel);
        badge.setStyle(
                "-fx-background-color: " + badgeBg + ";" +
                        "-fx-text-fill: " + badgeText + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 5 14;"
        );


        String vetNom = "Dr. ?";
        try { vetNom = "Dr. " + serviceRdv.getVetNom(rdv.getVet_id()); }
        catch (Exception ignored) {}

        Label vetLabel = new Label("👨‍⚕️ " + vetNom);
        vetLabel.setStyle(
                "-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #92400e;"
        );

        topRow.getChildren().addAll(badge, vetLabel);

        // Ligne 2 : créneau
        String creneauText = "-";
        try {
            var dispo = serviceDispo.findById(rdv.getDisponibilite_id());
            if (dispo != null && dispo.getStarttime() != null) {
                creneauText = "📅 " +
                        dispo.getStarttime().toLocalDate() + "  " +
                        dispo.getStarttime().toLocalTime().toString().substring(0, 5) +
                        " → " +
                        dispo.getEndtime().toLocalTime().toString().substring(0, 5);
            }
        } catch (Exception ignored) {}

        Label creneauLabel = new Label(creneauText);
        creneauLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #78716c;");

        // Ligne 3 : description
        Label descLabel = new Label("📝 " + rdv.getDescription());
        descLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
        descLabel.setWrapText(true);

        card.getChildren().addAll(topRow, creneauLabel, descLabel);
        return card;
    }

    @FXML
    private void goBack(ActionEvent event) {
        ViewNavigator.goTo(event, "/DashboardClient.fxml", "Mon Espace");
    }
}