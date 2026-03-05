package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Rendezvous;
import services.EmailService;
import services.ServiceRendezvous;
import services.SmsService;
import utils.SessionManager;
import utils.ViewNavigator;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RendezvousListController {

    @FXML private VBox cardsContainer;
    @FXML private ComboBox<String> filterBox;

    private final ServiceRendezvous serviceRendezvous = new ServiceRendezvous();
    private final EmailService emailService = new EmailService();
    private ObservableList<Rendezvous> allRendezvous = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        filterBox.setItems(FXCollections.observableArrayList(
                "Tous", "EN_ATTENTE", "CONFIRME", "ANNULE", "TERMINE"
        ));
        filterBox.setValue("Tous");
        filterBox.setOnAction(e -> renderCards());
        loadData();
    }

    private void loadData() {
        try {
            List<Rendezvous> list = serviceRendezvous.readByVetId(SessionManager.getUserId());
            allRendezvous = FXCollections.observableArrayList(list);
            renderCards();
        } catch (SQLException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        String filtre = filterBox.getValue();
        List<Rendezvous> filtered = allRendezvous.stream()
                .filter(r -> filtre == null || "Tous".equals(filtre) || filtre.equals(r.getStatus()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label("Aucun rendez-vous trouvé.");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 15; -fx-padding: 20;");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (Rendezvous rdv : filtered) {
            cardsContainer.getChildren().add(buildCard(rdv));
        }
    }

    private VBox buildCard(Rendezvous rdv) {

        // ✅ Couleurs selon statut
        String borderColor, badgeBg, badgeText, statusLabel;
        switch (rdv.getStatus()) {
            case "CONFIRME" -> {
                borderColor = "#22c55e"; badgeBg = "#dcfce7";
                badgeText = "#16a34a"; statusLabel = "✅ Confirmé";
            }
            case "ANNULE" -> {
                borderColor = "#ef4444"; badgeBg = "#fee2e2";
                badgeText = "#dc2626"; statusLabel = "❌ Annulé";
            }
            case "TERMINE" -> {
                borderColor = "#8b5cf6"; badgeBg = "#ede9fe";
                badgeText = "#7c3aed"; statusLabel = "🏁 Terminé";
            }
            default -> {
                borderColor = "#f59e0b"; badgeBg = "#fef3c7";
                badgeText = "#d97706"; statusLabel = "⏳ En attente";
            }
        }

        // ✅ Parser la description
        String description = rdv.getDescription();
        String animalInfo = "";
        String slotInfo   = "";
        String motif      = description;

        try {
            int a1 = description.indexOf("[");
            int a2 = description.indexOf("]");
            if (a1 >= 0 && a2 > a1) {
                animalInfo = description.substring(a1 + 1, a2)
                        .replace("🐾", "").trim();
                int s1 = description.indexOf("[", a2 + 1);
                int s2 = description.indexOf("]", s1 + 1);
                if (s1 >= 0 && s2 > s1) {
                    slotInfo = description.substring(s1 + 1, s2)
                            .replace("⏰", "").trim();
                    motif = description.substring(s2 + 1).trim();
                }
            }
        } catch (Exception ignored) {}

        // ✅ Carte principale
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

        // Ligne 1 : badge + téléphone
        HBox topRow = new HBox(12);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label badge = new Label(statusLabel);
        badge.setStyle(
                "-fx-background-color: " + badgeBg + ";" +
                        "-fx-text-fill: " + badgeText + ";" +
                        "-fx-background-radius: 8; -fx-font-weight: bold;" +
                        "-fx-font-size: 13; -fx-padding: 5 14;"
        );

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label phoneLabel = new Label("📞  " + (rdv.getNum() != 0 ? rdv.getNum() : "-"));
        phoneLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #78716c;");

        topRow.getChildren().addAll(badge, spacer, phoneLabel);

        Separator sep = new Separator();

        // Ligne 2 : Animal + Créneau
        HBox infoRow = new HBox(20);
        infoRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label animalLabel = new Label("🐾  " + (animalInfo.isEmpty() ? "-" : animalInfo));
        animalLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #92400e;");

        Label slotLabel = new Label("📅  " + (slotInfo.isEmpty() ? "-" : slotInfo));
        slotLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #f97316; -fx-font-weight: bold;");

        infoRow.getChildren().addAll(animalLabel, slotLabel);

        Label motifLabel = new Label("Motif :  " + (motif.isEmpty() ? "-" : motif));
        motifLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
        motifLabel.setWrapText(true);

        card.getChildren().addAll(topRow, sep, infoRow, motifLabel);

        // ✅ Boutons si EN_ATTENTE
        if ("EN_ATTENTE".equals(rdv.getStatus())) {
            HBox btnRow = new HBox(10);
            btnRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button btnAccepter = new Button("✅ Accepter");
            btnAccepter.setStyle(
                    "-fx-background-color: #22c55e; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-font-weight: bold;" +
                            "-fx-cursor: hand; -fx-padding: 8 24; -fx-font-size: 13;");
            btnAccepter.setOnAction(e -> updateStatus(rdv, "CONFIRME"));

            Button btnRefuser = new Button("❌ Refuser");
            btnRefuser.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-font-weight: bold;" +
                            "-fx-cursor: hand; -fx-padding: 8 24; -fx-font-size: 13;");
            btnRefuser.setOnAction(e -> updateStatus(rdv, "ANNULE"));

            btnRow.getChildren().addAll(btnAccepter, btnRefuser);
            card.getChildren().add(btnRow);
        }

        // ✅ Bouton Terminer si CONFIRME
        if ("CONFIRME".equals(rdv.getStatus())) {
            HBox btnRow = new HBox(10);
            btnRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button btnTerminer = new Button("🏁 Consultation terminée");
            btnTerminer.setStyle(
                    "-fx-background-color: #8b5cf6; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-font-weight: bold;" +
                            "-fx-cursor: hand; -fx-padding: 8 24; -fx-font-size: 13;");
            btnTerminer.setOnAction(e -> {
                // ✅ Confirmation avant de terminer
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmer");
                confirm.setHeaderText(null);
                confirm.setContentText("Confirmer la fin de cette consultation ?");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        updateStatusSilent(rdv, "TERMINE");
                    }
                });
            });

            btnRow.getChildren().add(btnTerminer);
            card.getChildren().add(btnRow);
        }

        return card;
    }

    private void updateStatus(Rendezvous rdv, String newStatus) {
        try {
            rdv.setStatus(newStatus);
            serviceRendezvous.update(rdv);

            String vetNom = SessionManager.getUserNom();

            // ✅ Email au client
            String clientEmail = serviceRendezvous.getClientEmail(rdv.getClient_id());
            if (clientEmail != null && !clientEmail.isEmpty()) {
                CompletableFuture.runAsync(() ->
                        emailService.notifyClientRdvStatus(
                                clientEmail, newStatus, vetNom, rdv.getDescription())
                );
            }

            // ✅ SMS au client
            String clientNum = String.valueOf(rdv.getNum());
            if (!clientNum.equals("0") && !clientNum.isEmpty()) {
                String msg = "CONFIRME".equals(newStatus)
                        ? "Bonjour! Votre RDV chez Dr." + vetNom + " est CONFIRME. FurHope."
                        : "Bonjour! Votre RDV chez Dr." + vetNom + " est ANNULE. FurHope.";
                String phone = clientNum.startsWith("216") || clientNum.startsWith("+216")
                        ? clientNum : "216" + clientNum;
                System.out.println("📱 Envoi SMS à : " + phone);
                CompletableFuture.runAsync(() -> SmsService.sendSms(phone, msg));
            }

            loadData();

        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    // ✅ Terminer sans SMS ni email
    private void updateStatusSilent(Rendezvous rdv, String newStatus) {
        try {
            rdv.setStatus(newStatus);
            serviceRendezvous.update(rdv);
            loadData();
        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() { loadData(); }

    @FXML
    private void onBackHome(ActionEvent event) {
        ViewNavigator.goTo(event, "/Dashboard.fxml", "Dashboard Vétérinaire");
    }
    @FXML
    private void onBackAccueil(ActionEvent event) {
        ViewNavigator.goTo(event, "/accueil.fxml", "FurHope");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setContentText(msg);
        a.showAndWait();
    }
}