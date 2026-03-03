package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Disponibilite;
import services.ServiceDisponibilite;

import java.sql.SQLException;
import java.util.List;

public class AfficherDisponibilite {

    @FXML
    private TableColumn<Disponibilite, Integer> IdCol;

    @FXML
    private TableColumn<Disponibilite, String> vetNomCol; // ✅ Nouvelle colonne pour le nom du vétérinaire

    @FXML
    private TableColumn<Disponibilite, String> startTimeCol;

    @FXML
    private TableColumn<Disponibilite, String> endTimeCol;

    @FXML
    private TableColumn<Disponibilite, Disponibilite.Statut> statutCol;

    @FXML
    private TableView<Disponibilite> tableView;

    ServiceDisponibilite ps = new ServiceDisponibilite();

    @FXML
    void initialize() {

        try {
            List<Disponibilite> disponibilites = ps.read();
            ObservableList<Disponibilite> observableList = FXCollections.observableList(disponibilites);
            tableView.setItems(observableList);

            // Lier les colonnes aux propriétés de Disponibilite
            IdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            vetNomCol.setCellValueFactory(new PropertyValueFactory<>("vetNom")); // ✅ liaison du nom
            startTimeCol.setCellValueFactory(new PropertyValueFactory<>("starttime"));
            endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endtime"));
            statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}