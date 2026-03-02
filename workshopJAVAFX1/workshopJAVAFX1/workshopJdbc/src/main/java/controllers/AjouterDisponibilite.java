package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import model.Disponibilite;
import services.ServiceDisponibilite;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.sql.SQLException;

public class AjouterDisponibilite {

    @FXML
    private TextField vet_idTF;

    @FXML
    private TextField starttimeTF;

    @FXML
    private TextField endtimeTF;

    @FXML
    private TextField statutTF;

    ServiceDisponibilite ps = new ServiceDisponibilite();

    @FXML
    void save(ActionEvent event) {
        try {
            int vetId = Integer.parseInt(vet_idTF.getText());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            LocalDateTime startTime = LocalDateTime.parse(starttimeTF.getText(), formatter);
            LocalDateTime endTime = LocalDateTime.parse(endtimeTF.getText(), formatter);

            Disponibilite.Statut statut;
            if (statutTF.getText().equalsIgnoreCase("VALABLE")) {
                statut = Disponibilite.Statut.VALABLE;
            } else {
                statut = Disponibilite.Statut.NONVALABLE;
            }


            Disponibilite dispo = new Disponibilite(vetId, startTime, endTime, statut);


            ps.add(dispo);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Disponibilité ajoutée !");
            alert.showAndWait();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Veuillez entrer un ID vétérinaire valide !");
            alert.showAndWait();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur SQL");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void naviguer(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherDisponibilite.fxml"));
            vet_idTF.getScene().setRoot(root); // utiliser un champ existant pour changer de scène
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}