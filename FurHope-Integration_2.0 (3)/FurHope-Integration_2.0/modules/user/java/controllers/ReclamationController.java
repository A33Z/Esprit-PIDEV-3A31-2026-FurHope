package controllers;

import com.esprit.entities.Reclamation;
import entities.User;
import com.esprit.services.ReclamationService;
import com.esprit.services.ReclamationAiAssistantService;
import com.esprit.services.ReponseService;
import com.esprit.entities.Reponse;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ReclamationController {

    @FXML
    private FlowPane reclamationCardsPane;
    @FXML
    private TextField sujetField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;

    private final ReclamationService service = new ReclamationService();
    private final ReponseService reponseService = new ReponseService();
    private final ReclamationAiAssistantService aiAssistant = new ReclamationAiAssistantService();
    private final ObservableList<Reclamation> sourceData = FXCollections.observableArrayList();
    private Reclamation selectedReclamation;

    @FXML
    private void initialize() {
        if (!SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }

        statusCombo.setItems(FXCollections.observableArrayList("OPEN", "IN_PROGRESS", "RESOLVED"));
        statusCombo.getSelectionModel().select("OPEN");
        sortCombo.setItems(FXCollections.observableArrayList("Newest first", "Oldest first", "Status A-Z", "Sujet A-Z"));
        sortCombo.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        refreshTable();

        if (!SessionContext.isAdmin()) {
            statusCombo.setDisable(true);
        }
    }

    @FXML
    private void addReclamation() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }
        if (sujetField.getText().trim().isEmpty() || descriptionArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Sujet and description are required.");
            return;
        }
        String textForModeration = sujetField.getText().trim() + " " + descriptionArea.getText().trim();
        String badWord = aiAssistant.findFirstProfanity(textForModeration);
        if (badWord != null) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Inappropriate language detected: " + badWord);
            return;
        }

        try {
            Reclamation reclamation = new Reclamation();
            reclamation.setClientId(user.getId());
            reclamation.setSujet(sujetField.getText().trim());
            reclamation.setDescription(descriptionArea.getText().trim());
            reclamation.setStatus("OPEN");
            service.ajouter(reclamation);
            addImmediateAiReply(reclamation);
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to add reclamation.");
        }
    }

    @FXML
    private void updateReclamation() {
        if (selectedReclamation == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation card to update.");
            return;
        }
        if (!canModify(selectedReclamation)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You can only edit your own reclamations.");
            return;
        }

        try {
            String textForModeration = sujetField.getText().trim() + " " + descriptionArea.getText().trim();
            String badWord = aiAssistant.findFirstProfanity(textForModeration);
            if (badWord != null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Inappropriate language detected: " + badWord);
                return;
            }
            selectedReclamation.setSujet(sujetField.getText().trim());
            selectedReclamation.setDescription(descriptionArea.getText().trim());
            if (SessionContext.isAdmin()) {
                selectedReclamation.setStatus(statusCombo.getValue());
            }
            service.modifier(selectedReclamation);
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update reclamation.");
        }
    }

    @FXML
    private void deleteReclamation() {
        if (selectedReclamation == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation card to delete.");
            return;
        }
        if (!canModify(selectedReclamation)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You can only delete your own reclamations.");
            return;
        }

        try {
            service.supprimer(selectedReclamation.getId());
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to delete reclamation.");
        }
    }

    @FXML
    private void openResponses(ActionEvent event) {
        if (selectedReclamation == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation card first.");
            return;
        }
        SessionContext.setSelectedReclamationId(selectedReclamation.getId());
        switchScene(event, "/reponse.fxml");
    }

    @FXML
    private void refreshTable() {
        try {
            if (SessionContext.isAdmin()) {
                sourceData.setAll(service.afficher());
            } else {
                sourceData.setAll(service.afficherParClient(SessionContext.getCurrentUser().getId()));
            }
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load reclamations.");
        }
    }

    @FXML
    private void applyFilters() {
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        ObservableList<Reclamation> filtered = FXCollections.observableArrayList(
                sourceData.filtered(rec -> matchesSearch(rec, query))
        );
        FXCollections.sort(filtered, buildComparator(sortCombo == null ? null : sortCombo.getValue()));
        renderCards(filtered);
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
    }

    private void renderCards(List<Reclamation> list) {
        reclamationCardsPane.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label empty = new Label("No reclamations found.");
            empty.getStyleClass().add("card-subtitle");
            reclamationCardsPane.getChildren().add(empty);
            return;
        }
        for (Reclamation rec : list) {
            reclamationCardsPane.getChildren().add(createCard(rec));
        }
    }

    private VBox createCard(Reclamation rec) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "reclamation-card");
        if (selectedReclamation != null && selectedReclamation.getId() == rec.getId()) {
            card.getStyleClass().add("selected-reclamation-card");
        }
        card.setPadding(new Insets(12));
        card.setPrefWidth(320);

        Label subject = new Label(safe(rec.getSujet(), "No subject"));
        subject.getStyleClass().add("card-title");
        subject.setWrapText(true);

        Label desc = new Label(safe(rec.getDescription(), "No description"));
        desc.getStyleClass().add("card-subtitle");
        desc.setWrapText(true);

        Label meta = new Label("Client #" + rec.getClientId() + "  •  " + formatDate(rec.getCreatedAt()));
        meta.getStyleClass().add("field-label");

        Label status = new Label(safe(rec.getStatus(), "OPEN"));
        status.getStyleClass().addAll("status-badge", statusClass(rec.getStatus()));

        Button select = new Button("Select");
        select.getStyleClass().addAll("secondary-button", "action-button-small");
        select.setOnAction(e -> selectReclamation(rec));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, status, spacer, select);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.setOnMouseClicked(e -> selectReclamation(rec));
        card.getChildren().addAll(subject, desc, meta, actions);
        return card;
    }

    private void selectReclamation(Reclamation rec) {
        selectedReclamation = rec;
        fillForm(rec);
        applyFilters();
    }

    private boolean canModify(Reclamation reclamation) {
        return SessionContext.isAdmin() || SessionContext.getCurrentUser().getId() == reclamation.getClientId();
    }

    private void fillForm(Reclamation reclamation) {
        if (reclamation == null) {
            return;
        }
        sujetField.setText(reclamation.getSujet());
        descriptionArea.setText(reclamation.getDescription());
        statusCombo.getSelectionModel().select(reclamation.getStatus());
    }

    private void clearForm() {
        selectedReclamation = null;
        sujetField.clear();
        descriptionArea.clear();
        statusCombo.getSelectionModel().select("OPEN");
    }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
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

    private boolean matchesSearch(Reclamation rec, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(rec.getSujet(), query)
                || containsIgnoreCase(rec.getDescription(), query)
                || containsIgnoreCase(rec.getStatus(), query)
                || String.valueOf(rec.getClientId()).contains(query)
                || String.valueOf(rec.getId()).contains(query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<Reclamation> buildComparator(String selectedSort) {
        if ("Oldest first".equals(selectedSort)) {
            return Comparator.comparing(this::safeCreatedAt);
        }
        if ("Status A-Z".equals(selectedSort)) {
            return Comparator.comparing(rec -> safe(rec.getStatus(), "").toLowerCase(Locale.ROOT));
        }
        if ("Sujet A-Z".equals(selectedSort)) {
            return Comparator.comparing(rec -> safe(rec.getSujet(), "").toLowerCase(Locale.ROOT));
        }
        return Comparator.comparing(this::safeCreatedAt).reversed();
    }

    private LocalDateTime safeCreatedAt(Reclamation rec) {
        return rec.getCreatedAt() == null ? LocalDateTime.MIN : rec.getCreatedAt();
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return "No date";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String statusClass(String status) {
        String s = safe(status, "OPEN").toUpperCase(Locale.ROOT);
        if ("RESOLVED".equals(s)) {
            return "status-approved";
        }
        if ("IN_PROGRESS".equals(s)) {
            return "status-pending";
        }
        return "status-declined";
    }

    private void addImmediateAiReply(Reclamation reclamation) {
        try {
            if (reclamation == null || reclamation.getId() <= 0) {
                return;
            }
            Reponse aiReply = new Reponse();
            aiReply.setReclamationId(reclamation.getId());
            aiReply.setAdminId(-1);
            aiReply.setSenderId(-1);
            aiReply.setSenderType("AI");
            aiReply.setMessage(aiAssistant.buildImmediateReply(reclamation));
            aiReply.setRating(null);
            reponseService.ajouter(aiReply);
        } catch (Exception e) {
            System.err.println("AI auto-reply warning: " + e.getMessage());
        }
    }
}
