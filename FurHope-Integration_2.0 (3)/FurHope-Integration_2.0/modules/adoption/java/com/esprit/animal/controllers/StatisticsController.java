package com.esprit.animal.controllers;

import com.esprit.animal.Services.StatisticsService;
import com.esprit.animal.entities.AnimalStatistics;
import com.esprit.animal.i18n.LanguageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class StatisticsController extends BaseUIController {

    @FXML
    private Label totalAnimalsLabel;
    @FXML
    private Label availableAnimalsLabel;
    @FXML
    private Label adoptedAnimalsLabel;
    @FXML
    private Label totalRequestsLabel;
    @FXML
    private Label pendingRequestsLabel;
    @FXML
    private Label approvedRequestsLabel;
    @FXML
    private Label rejectedRequestsLabel;
    @FXML
    private Label mostCommonSpeciesLabel;

    private final StatisticsService statisticsService = new StatisticsService();

    @Override
    protected String getViewPath() {
        return "/animal/StatisticsDashboard.fxml";
    }

    @Override
    protected String getBackViewPath() {
        return "/animal/favoriteAnimal.fxml";
    }

    @FXML
    public void initialize() {
        loadStatistics();
    }

    @FXML
    private void handleRefresh() {
        loadStatistics();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) totalAnimalsLabel.getScene().getWindow();
        stage.close();
    }

    private void loadStatistics() {
        try {
            AnimalStatistics stats = statisticsService.getAnimalStatistics();

            totalAnimalsLabel.setText(String.valueOf(stats.getTotalAnimals()));
            availableAnimalsLabel.setText(String.valueOf(stats.getAvailableAnimals()));
            adoptedAnimalsLabel.setText(String.valueOf(stats.getAdoptedAnimals()));
            totalRequestsLabel.setText(String.valueOf(stats.getTotalRequests()));
            pendingRequestsLabel.setText(String.valueOf(stats.getPendingRequests()));
            approvedRequestsLabel.setText(String.valueOf(stats.getApprovedRequests()));
            rejectedRequestsLabel.setText(String.valueOf(stats.getRejectedRequests()));
            mostCommonSpeciesLabel.setText(stats.getMostCommonSpecies());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(tr("stats.error.title"));
            alert.setHeaderText(tr("stats.error.header"));
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private String tr(String key) {
        try {
            return LanguageManager.get(key);
        } catch (Exception e) {
            return key;
        }
    }
}

