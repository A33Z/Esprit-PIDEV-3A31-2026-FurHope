package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.ServiceReview;
import utils.MyDatabase;
import utils.SessionManager;
import utils.ViewNavigator;

import java.sql.*;

public class ListeVeterinairesController {

    @FXML private VBox vetsContainer;
    private final ServiceReview serviceReview = new ServiceReview();

    @FXML
    public void initialize() {
        loadVeterinaires();
    }

    private void loadVeterinaires() {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, first_name, last_name, email, phone, city " +
                            "FROM user WHERE role = 'VETERINAIRE' AND active = 1"
            );
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                int id     = rs.getInt("id");
                String nom = "Dr. " + rs.getString("first_name") +
                        " " + rs.getString("last_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone") != null ?
                        rs.getString("phone") : "Non renseigné";
                String city  = rs.getString("city")  != null ?
                        rs.getString("city")  : "Non renseignée";

                double avg     = serviceReview.getAverageRating(id);
                int nbAvis     = serviceReview.getNombreAvis(id);
                int nbRdv      = getNombreRdv(id); // ✅ Compteur RDV
                String etoiles = buildEtoiles((int) Math.round(avg));
                String avisLabel = nbAvis > 0
                        ? String.format("%.1f/5 (%d avis)", avg, nbAvis)
                        : "Pas encore d'avis";

                vetsContainer.getChildren().add(
                        createVetCard(id, nom, email, phone, city, etoiles, avisLabel, nbRdv) // ✅
                );
            }

            if (!found) {
                Label label = new Label("Aucun vétérinaire disponible.");
                label.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14;");
                vetsContainer.getChildren().add(label);
            }

        } catch (Exception e) {
            Label err = new Label("❌ Erreur : " + e.getMessage());
            err.setStyle("-fx-text-fill: red;");
            vetsContainer.getChildren().add(err);
        }
    }

    // ✅ nbRdv ajouté en paramètre
    private VBox createVetCard(int vetId, String nom, String email,
                               String phone, String city,
                               String etoiles, String avisLabel, int nbRdv) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14;" +
                        "-fx-padding: 20;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);"
        );

        HBox ligne1 = new HBox(14);
        ligne1.setStyle("-fx-alignment: CENTER_LEFT;");

        Label avatar = new Label("👨‍⚕️");
        avatar.setStyle("-fx-font-size: 36;");

        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        Label labelNom = new Label(nom);
        labelNom.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #92400e;");

        // Étoiles + score
        HBox ratingBox = new HBox(8);
        ratingBox.setStyle("-fx-alignment: CENTER_LEFT;");
        Label labelEtoiles = new Label(etoiles.isEmpty() ? "☆☆☆☆☆" : etoiles);
        labelEtoiles.setStyle("-fx-font-size: 15;");
        Label labelScore = new Label(avisLabel);
        labelScore.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        ratingBox.getChildren().addAll(labelEtoiles, labelScore);

        // ✅ Compteur RDV
        Label labelRdv = new Label("📋 " + nbRdv + " rendez-vous");
        labelRdv.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");

        Label labelCity = new Label("📍 " + city);
        labelCity.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");

        infos.getChildren().addAll(labelNom, ratingBox, labelRdv, labelCity);
        ligne1.getChildren().addAll(avatar, infos);

        HBox ligne2 = new HBox(20);
        Label labelEmail = new Label("✉️ " + email);
        labelEmail.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        Label labelPhone = new Label("📞 " + phone);
        labelPhone.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        ligne2.getChildren().addAll(labelEmail, labelPhone);

        HBox sep = new HBox();
        sep.setStyle("-fx-background-color: #ffedd5; -fx-min-height: 1;");

        Button btnRdv = new Button("📅 Prendre un Rendez-vous");
        btnRdv.setStyle(
                "-fx-background-color: #f97316; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10;" +
                        "-fx-padding: 10 24; -fx-cursor: hand; -fx-font-size: 13;"
        );
        btnRdv.setOnAction(e -> {
            SessionManager.setSelectedVetId(vetId);
            SessionManager.setSelectedVetNom(nom);
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/RendezvousForm.fxml")
                );
                Parent root = loader.load();
                Stage stage = (Stage) vetsContainer.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Prendre un RDV");
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setContentText("Erreur: " + ex.getMessage() +
                        "\nCause: " + (ex.getCause() != null ?
                        ex.getCause().getMessage() : "inconnue"));
                alert.showAndWait();
            }
        });

        card.getChildren().addAll(ligne1, ligne2, sep, btnRdv);
        return card;
    }

    private String buildEtoiles(int rating) {
        if (rating <= 0) return "☆☆☆☆☆";
        return "⭐".repeat(Math.min(rating, 5)) +
                "☆".repeat(Math.max(0, 5 - rating));
    }

    private int getNombreRdv(int vetId) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM rendezvous WHERE vet_id = ? AND status != 'ANNULE'"
            );
            ps.setInt(1, vetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    @FXML
    private void goBack(ActionEvent event) {
        ViewNavigator.goTo(event, "/DashboardClient.fxml", "Mon Espace");
    }
}
