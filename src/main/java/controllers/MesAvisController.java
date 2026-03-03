package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.ServiceReview;
import utils.MyDatabase;
import utils.SessionManager;
import utils.ViewNavigator;

import java.sql.*;

public class MesAvisController {

    @FXML private VBox avisContainer;
    private final ServiceReview serviceReview = new ServiceReview();

    @FXML
    public void initialize() {
        loadRdvANoter();
    }

    private void loadRdvANoter() {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            String sql = "SELECT r.id_rdv, r.vet_id, r.description, " +
                    "u.first_name, u.last_name " +
                    "FROM rendezvous r " +
                    "JOIN user u ON u.id = r.vet_id " +
                    "LEFT JOIN review rv ON rv.rdv_id = r.id_rdv " +
                    "WHERE r.client_id = ? AND r.status = 'TERMINE' " +
                    "AND rv.id IS NULL";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, SessionManager.getUserId());
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            while (rs.next()) {
                found = true;
                int rdvId  = rs.getInt("id_rdv");
                int vetId  = rs.getInt("vet_id");
                String vetNom = "Dr. " + rs.getString("first_name") +
                        " " + rs.getString("last_name");

                avisContainer.getChildren().add(
                        createAvisCard(rdvId, vetId, vetNom)
                );
            }

            if (!found) {
                Label label = new Label("✅ Aucun avis en attente — merci pour votre fidélité !");
                label.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14; -fx-padding: 20;");
                avisContainer.getChildren().add(label);
            }

        } catch (Exception e) {
            Label err = new Label("❌ Erreur : " + e.getMessage());
            err.setStyle("-fx-text-fill: red;");
            avisContainer.getChildren().add(err);
        }
    }

    // ✅ Plus de description en paramètre
    private VBox createAvisCard(int rdvId, int vetId, String vetNom) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-padding: 24;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);");

        // ✅ Nom vétérinaire
        Label labelVet = new Label("👨‍⚕️ " + vetNom);
        labelVet.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #92400e;");

        // ✅ Message d'introduction — sans données brutes
        Label labelIntro = new Label(
                "Cher(e) client(e), votre avis sur " + vetNom + " compte beaucoup pour nous.\n" +
                        "Merci d'être honnête et de nous aider à améliorer continuellement " +
                        "la qualité de nos services. 🐾"
        );
        labelIntro.setWrapText(true);
        labelIntro.setStyle(
                "-fx-font-size: 13; -fx-text-fill: #6b7280; -fx-font-style: italic;" +
                        "-fx-background-color: #fef3c7; -fx-background-radius: 8;" +
                        "-fx-padding: 12; -fx-border-color: #fde68a; -fx-border-radius: 8;"
        );


        Label labelRating = new Label("Votre note :");
        labelRating.setStyle("-fx-font-weight: bold; -fx-text-fill: #92400e; -fx-font-size: 14;");

        final int[] selectedRating = {0};
        HBox starsBox = new HBox(8);
        starsBox.setStyle("-fx-alignment: CENTER_LEFT;");
        Button[] stars = new Button[5];

        for (int i = 0; i < 5; i++) {
            final int starValue = i + 1;
            Button star = new Button("☆");
            star.setStyle("-fx-background-color: transparent; -fx-font-size: 28;" +
                    "-fx-cursor: hand; -fx-text-fill: #eab308; -fx-padding: 0;");
            star.setOnAction(e -> {
                selectedRating[0] = starValue;
                for (int j = 0; j < 5; j++) {
                    stars[j].setText(j < starValue ? "⭐" : "☆");
                }
            });
            stars[i] = star;
            starsBox.getChildren().add(star);
        }


        TextArea commentaire = new TextArea();
        commentaire.setPromptText("Partagez votre expérience... (optionnel)");
        commentaire.setPrefRowCount(3);
        commentaire.setStyle("-fx-background-color: #fff7ed; -fx-border-color: #fed7aa;" +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");


        Button btnSoumettre = new Button("✅ Soumettre mon avis");
        btnSoumettre.setStyle("-fx-background-color: #eab308; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-background-radius: 10;" +
                "-fx-padding: 12 28; -fx-cursor: hand; -fx-font-size: 14;");
        btnSoumettre.setOnAction(e -> {
            if (selectedRating[0] == 0) {
                errorLabel.setText("⚠️ Choisissez une note !");
                return;
            }
            try {
                model.Review review = new model.Review();
                review.setClientId(SessionManager.getUserId());
                review.setVetId(vetId);
                review.setRdvId(rdvId);
                review.setRating(selectedRating[0]);
                review.setCommentaire(commentaire.getText().trim());
                serviceReview.add(review);

                // ✅ Remplacer la carte par succès
                card.getChildren().clear();
                VBox successBox = new VBox(8);
                successBox.setStyle("-fx-alignment: CENTER;");
                Label success = new Label("✅ Merci pour votre avis !");
                success.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 16;" +
                        "-fx-font-weight: bold;");
                Label successSub = new Label("Votre avis sur " + vetNom +
                        " a bien été enregistré. 🐾");
                successSub.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13;");
                successBox.getChildren().addAll(success, successSub);
                card.getChildren().add(successBox);
                card.setStyle("-fx-background-color: #f0fdf4; " +
                        "-fx-background-radius: 14; -fx-padding: 24;" +
                        "-fx-border-color: #22c55e; -fx-border-radius: 14;" +
                        "-fx-border-width: 2;");

            } catch (Exception ex) {
                errorLabel.setText("❌ Erreur : " + ex.getMessage());
            }
        });

        card.getChildren().addAll(
                labelVet,
                labelIntro,
                new Separator(),
                labelRating,
                starsBox,
                commentaire,
                errorLabel,
                btnSoumettre
        );
        return card;
    }

    @FXML
    private void goBack(ActionEvent event) {
        ViewNavigator.goTo(event, "/DashboardClient.fxml", "Mon Espace");
    }
}
