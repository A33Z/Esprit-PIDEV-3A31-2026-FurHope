package controllers;

import com.esprit.entities.Reclamation;
import com.esprit.entities.Reponse;
import entities.User;
import com.esprit.services.ReclamationService;
import com.esprit.services.ReponseService;
import com.esprit.services.ai.AIResponseService;
import com.esprit.services.ai.ReclamationAutoReplyConfig;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;

public class ReclamationController {

    @FXML
    private ListView<Reclamation> reclamationTable;

    @FXML
    private TextField sujetField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private ComboBox<String> filterStatusCombo;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    private final ReclamationService service = new ReclamationService();
    private final ReponseService reponseService = new ReponseService();
    private final AIResponseService aiResponseService = new AIResponseService();
    private final ReclamationAutoReplyConfig aiConfig = ReclamationAutoReplyConfig.load();
    private final ObservableList<Reclamation> sourceData = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        if (!controllers.SessionContext.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }

        reclamationTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Reclamation reclamation, boolean empty) {
                super.updateItem(reclamation, empty);
                if (empty || reclamation == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(buildTicketCard(reclamation));
            }
        });

        statusCombo.setItems(FXCollections.observableArrayList("OPEN", "IN_PROGRESS", "RESOLVED", "REVIEWED", "CLOSED"));
        statusCombo.getSelectionModel().select("OPEN");
        filterStatusCombo.setItems(FXCollections.observableArrayList("All Statuses", "OPEN", "IN_PROGRESS", "RESOLVED", "REVIEWED", "CLOSED"));
        filterStatusCombo.getSelectionModel().selectFirst();
        sortCombo.setItems(FXCollections.observableArrayList("Newest first", "Oldest first", "Status A-Z", "Sujet A-Z"));
        sortCombo.getSelectionModel().selectFirst();

        reclamationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, selected) -> fillForm(selected));
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        filterStatusCombo.valueProperty().addListener((obs, oldText, newText) -> applyFilters());
        refreshTable();

        if (!controllers.SessionContext.isAdmin()) {
            statusCombo.setDisable(true);
        }
    }

    @FXML
    private void addReclamation() {
        User user = controllers.SessionContext.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please login first.");
            return;
        }

        if (sujetField.getText().trim().isEmpty() || descriptionArea.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Sujet and description are required.");
            return;
        }

        try {
            Reclamation reclamation = new Reclamation();
            reclamation.setClientId(user.getId());
            reclamation.setSujet(sujetField.getText().trim());
            reclamation.setDescription(descriptionArea.getText().trim());
            reclamation.setStatus("OPEN");
            service.ajouter(reclamation);
            generateAutoAiResponse(reclamation);
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to add reclamation.");
        }
    }

    @FXML
    private void updateReclamation() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation to update.");
            return;
        }

        if (!canModify(selected)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You can only edit your own reclamations.");
            return;
        }

        try {
            selected.setSujet(sujetField.getText().trim());
            selected.setDescription(descriptionArea.getText().trim());
            if (controllers.SessionContext.isAdmin()) {
                selected.setStatus(statusCombo.getValue());
            }
            service.modifier(selected);
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to update reclamation.");
        }
    }

    @FXML
    private void deleteReclamation() {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation to delete.");
            return;
        }

        if (!canModify(selected)) {
            showAlert(Alert.AlertType.WARNING, "Access Denied", "You can only delete your own reclamations.");
            return;
        }

        try {
            service.supprimer(selected.getId());
            clearForm();
            refreshTable();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to delete reclamation.");
        }
    }

    @FXML
    private void openResponses(ActionEvent event) {
        Reclamation selected = reclamationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a reclamation first.");
            return;
        }
        controllers.SessionContext.setSelectedReclamationId(selected.getId());
        switchScene(event, "/reponse.fxml");
    }

    @FXML
    private void refreshTable() {
        try {
            if (controllers.SessionContext.isAdmin()) {
                sourceData.setAll(service.afficher());
            } else {
                sourceData.setAll(service.afficherParClient(controllers.SessionContext.getCurrentUser().getId()));
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
        String selectedStatus = filterStatusCombo == null ? "All Statuses" : filterStatusCombo.getValue();
        if (selectedStatus != null && !"All Statuses".equalsIgnoreCase(selectedStatus)) {
            filtered.removeIf(rec -> rec.getStatus() == null || !selectedStatus.equalsIgnoreCase(rec.getStatus().trim()));
        }
        FXCollections.sort(filtered, buildComparator(sortCombo == null ? null : sortCombo.getValue()));
        reclamationTable.setItems(filtered);
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
    }

    private boolean canModify(Reclamation reclamation) {
        return controllers.SessionContext.isAdmin() || controllers.SessionContext.getCurrentUser().getId() == reclamation.getClientId();
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

    private String formatReclamation(Reclamation reclamation) {
        return String.format(
                "ID: %d | Client: %d | Sujet: %s | Status: %s | Created: %s",
                reclamation.getId(),
                reclamation.getClientId(),
                reclamation.getSujet(),
                reclamation.getStatus(),
                reclamation.getCreatedAt()
        );
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
            return Comparator.comparing(rec -> safeString(rec.getStatus()));
        }
        if ("Sujet A-Z".equals(selectedSort)) {
            return Comparator.comparing(rec -> safeString(rec.getSujet()));
        }
        return Comparator.comparing(this::safeCreatedAt).reversed();
    }

    private LocalDateTime safeCreatedAt(Reclamation rec) {
        return rec.getCreatedAt() == null ? LocalDateTime.MIN : rec.getCreatedAt();
    }

    private String safeString(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private VBox buildTicketCard(Reclamation reclamation) {
        Label subjectLabel = new Label(safeText(reclamation.getSujet()));
        subjectLabel.getStyleClass().add("ticket-subject");

        Label clientLabel = new Label("Client #" + reclamation.getClientId());
        clientLabel.getStyleClass().add("ticket-meta");

        Label statusBadge = new Label(normalizeStatus(reclamation.getStatus()));
        statusBadge.getStyleClass().addAll("badge", resolveBadgeStyle(reclamation.getStatus()));

        Label createdLabel = new Label(formatDate(reclamation.getCreatedAt()));
        createdLabel.getStyleClass().add("ticket-meta");

        HBox metaRow = new HBox(10, clientLabel, statusBadge);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(10, subjectLabel, spacer, createdLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label descLabel = new Label(safeText(reclamation.getDescription()));
        descLabel.getStyleClass().add("ticket-desc");
        descLabel.setWrapText(true);

        VBox root = new VBox(8, headerRow, metaRow, descLabel);
        root.getStyleClass().add("ticket-card");
        root.setPadding(new Insets(20));
        return root;
    }

    private String resolveBadgeStyle(String status) {
        String normalized = normalizeStatus(status);
        if ("OPEN".equals(normalized)) {
            return "badge-open";
        }
        if ("REVIEWED".equals(normalized) || "IN_PROGRESS".equals(normalized)) {
            return "badge-reviewed";
        }
        return "badge-closed";
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Created: -";
        }
        return "Created: " + DATE_FORMAT.format(dateTime);
    }

    private String normalizeStatus(String status) {
        return status == null ? "UNKNOWN" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void generateAutoAiResponse(Reclamation reclamation) {
        if (reclamation == null || reclamation.getId() <= 0) {
            return;
        }
        try {
            String complaint = buildComplaintText(reclamation);
            if (complaint.isBlank()) {
                return;
            }

            String generated = aiResponseService.generateSmartResponse(complaint);
            if (generated == null || generated.isBlank()) {
                return;
            }

            int systemAdminId = aiConfig.getSystemAdminId() > 0 ? aiConfig.getSystemAdminId() : 1;
            Reponse aiReponse = new Reponse();
            aiReponse.setReclamationId(reclamation.getId());
            aiReponse.setAdminId(systemAdminId);
            aiReponse.setMessage(generated.trim());
            reponseService.ajouter(aiReponse);

            if ("OPEN".equalsIgnoreCase(safeText(reclamation.getStatus()))) {
                reclamation.setStatus("REVIEWED");
                service.modifier(reclamation);
            }
        } catch (Exception ignored) {
            // Keep reclamation creation successful even if AI reply fails.
        }
    }

    private String buildComplaintText(Reclamation reclamation) {
        String subject = safeText(reclamation.getSujet());
        String description = safeText(reclamation.getDescription());
        if (subject.isBlank() && description.isBlank()) {
            return "";
        }
        if (subject.isBlank()) {
            return description;
        }
        if (description.isBlank()) {
            return "Subject: " + subject;
        }
        return "Subject: " + subject + "\nComplaint: " + description;
    }
}
