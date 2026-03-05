package controllers;

import controllers.SessionContext;
import entities.User;
import com.esprit.services.userservices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class UserAdminController {

    @FXML
    private FlowPane userCardsPane;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    private final userservices service = new userservices();
    private final ObservableList<User> sourceData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (!SessionContext.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "Only admin can manage users.");
            return;
        }

        sortCombo.setItems(FXCollections.observableArrayList("Newest ID", "Oldest ID", "Name A-Z", "Role A-Z"));
        sortCombo.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());

        refreshTable();
    }

    @FXML
    private void refreshTable() {
        try {
            sourceData.setAll(service.afficher());
            sourceData.removeIf(this::isAdminUser);
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load users.");
        }
    }

    @FXML
    private void applyFilters() {
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        ObservableList<User> filtered = FXCollections.observableArrayList(
                sourceData.filtered(user -> matchesSearch(user, query))
        );
        FXCollections.sort(filtered, buildComparator(sortCombo == null ? null : sortCombo.getValue()));
        renderUserCards(filtered);
    }

    private void updateActive(User target, boolean active) {
        if (target == null) {
            return;
        }
        if (isAdminUser(target)) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "Admin users cannot be modified.");
            return;
        }
        if (SessionContext.getCurrentUser() != null && target.getId() == SessionContext.getCurrentUser().getId()) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "You cannot modify your own active status.");
            return;
        }

        try {
            service.setActive(target.getId(), active);
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Updated", "User status updated.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update user status.");
        }
    }

    private void deleteUser(User target) {
        if (target == null) {
            return;
        }
        if (isAdminUser(target)) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "Admin users cannot be deleted.");
            return;
        }
        if (SessionContext.getCurrentUser() != null && target.getId() == SessionContext.getCurrentUser().getId()) {
            showAlert(Alert.AlertType.WARNING, "Action Blocked", "You cannot delete your own account.");
            return;
        }

        try {
            service.supprimer(target.getId());
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Deleted", "User deleted.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to delete user.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
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

    private void renderUserCards(List<User> users) {
        userCardsPane.getChildren().clear();
        if (users == null || users.isEmpty()) {
            Label empty = new Label("No users found.");
            empty.getStyleClass().add("card-subtitle");
            userCardsPane.getChildren().add(empty);
            return;
        }
        for (User user : users) {
            userCardsPane.getChildren().add(createUserCard(user));
        }
    }

    private VBox createUserCard(User user) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "user-card-block");
        card.setPadding(new Insets(14));
        card.setPrefWidth(310);

        Label name = new Label(displayName(user));
        name.getStyleClass().add("card-title");

        Label email = new Label(safeDisplay(user.getEmail(), "No email"));
        email.getStyleClass().add("card-subtitle");
        email.setWrapText(true);

        Label role = new Label("Role: " + safeDisplay(user.getRole(), "-"));
        role.getStyleClass().add("field-label");

        Label phone = new Label("Phone: " + safeDisplay(user.getPhone(), "-"));
        phone.getStyleClass().add("field-label");

        Label status = new Label(user.isActive() ? "ACTIVE" : "BLOCKED");
        status.getStyleClass().addAll("status-badge", user.isActive() ? "status-approved" : "status-declined");

        Button activateBtn = new Button("Activate");
        activateBtn.getStyleClass().addAll("primary-button", "action-button-small");
        activateBtn.setOnAction(e -> updateActive(user, true));
        activateBtn.setDisable(user.isActive());

        Button blockBtn = new Button("Block");
        blockBtn.getStyleClass().addAll("secondary-button", "action-button-small");
        blockBtn.setOnAction(e -> updateActive(user, false));
        blockBtn.setDisable(!user.isActive());

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("danger-button", "action-button-small");
        deleteBtn.setOnAction(e -> deleteUser(user));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, activateBtn, blockBtn, spacer, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(name, email, role, phone, status, actions);
        return card;
    }

    private String displayName(User user) {
        String first = safeDisplay(user == null ? null : user.getFirstName(), "");
        String last = safeDisplay(user == null ? null : user.getLastName(), "");
        String full = (first + " " + last).trim();
        if (!full.isEmpty()) {
            return full;
        }
        return "User #" + (user == null ? "-" : user.getId());
    }

    private String safeDisplay(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private boolean isAdminUser(User user) {
        return user != null && user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().trim());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private boolean matchesSearch(User user, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(user.getFirstName(), query)
                || containsIgnoreCase(user.getLastName(), query)
                || containsIgnoreCase(user.getEmail(), query)
                || containsIgnoreCase(user.getRole(), query)
                || String.valueOf(user.getId()).contains(query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<User> buildComparator(String selectedSort) {
        if ("Oldest ID".equals(selectedSort)) {
            return Comparator.comparingInt(User::getId);
        }
        if ("Name A-Z".equals(selectedSort)) {
            return Comparator.comparing(this::fullNameKey);
        }
        if ("Role A-Z".equals(selectedSort)) {
            return Comparator.comparing(user -> safeString(user.getRole()));
        }
        return Comparator.comparingInt(User::getId).reversed();
    }

    private String fullNameKey(User user) {
        return safeString(user.getFirstName()) + " " + safeString(user.getLastName());
    }

    private String safeString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
