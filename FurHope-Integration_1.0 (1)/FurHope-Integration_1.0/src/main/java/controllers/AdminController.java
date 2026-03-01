package controllers;

import entities.User;
import com.esprit.services.userservices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminController {

    @FXML
    private FlowPane pendingCardsPane;

    private final userservices service = new userservices();

    @FXML
    private void initialize() {
        refreshTable();
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
    }

    @FXML
    private void refreshTable() {
        try {
            ObservableList<User> data = FXCollections.observableArrayList(service.getPendingVets());
            renderPendingCards(data);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load pending users.");
        }
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", "Unable to open page.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private void renderPendingCards(ObservableList<User> users) {
        if (pendingCardsPane == null) {
            return;
        }
        pendingCardsPane.getChildren().clear();
        for (User user : users) {
            pendingCardsPane.getChildren().add(buildPendingCard(user));
        }
    }

    private VBox buildPendingCard(User user) {
        Label nameLabel = new Label((safeText(user.getFirstName()) + " " + safeText(user.getLastName())).trim());
        nameLabel.getStyleClass().add("card-title");

        Label roleLabel = new Label("Role: " + safeText(user.getRole()));
        roleLabel.getStyleClass().add("card-subtitle");

        Label emailLabel = new Label("Email: " + safeText(user.getEmail()));
        emailLabel.getStyleClass().add("card-subtitle");

        Label phoneLabel = new Label("Phone: " + safeText(user.getPhone()));
        phoneLabel.getStyleClass().add("card-subtitle");

        Button approveBtn = new Button("Approve");
        approveBtn.getStyleClass().addAll("primary-button", "action-button");
        approveBtn.setOnAction(e -> approveUser(user));

        HBox actions = new HBox(10, approveBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, nameLabel, roleLabel, emailLabel, phoneLabel, actions);
        card.getStyleClass().add("card-box");
        card.setPadding(new Insets(14));
        card.setPrefWidth(340);
        card.setMinWidth(320);
        return card;
    }

    private void approveUser(User user) {
        if (user == null) {
            return;
        }
        try {
            service.approveUser(user.getId());
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Approved", "User approved.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not approve user.");
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
