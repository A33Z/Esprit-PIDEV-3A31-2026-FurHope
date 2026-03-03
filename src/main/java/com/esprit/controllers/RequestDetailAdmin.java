package com.esprit.controllers;

import com.esprit.Services.adoptionservices;
import com.esprit.Services.animalServices;
import com.esprit.Services.BrevoService;
import com.esprit.entities.adoptionRequest;
import com.esprit.entities.animal;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;

public class RequestDetailAdmin {

    @FXML private Label animalNameLabel;
    @FXML private Label animalSpeciesLabel;
    @FXML private Label animalBreedLabel;
    @FXML private Label animalAgeGenderLabel;
    @FXML private ImageView animalImageView;

    @FXML private Label requesterNameLabel;
    @FXML private Label requesterEmailLabel;
    @FXML private Label requesterPhoneLabel;
    @FXML private Label requesterAddressLabel;

    @FXML private Label statusLabel;
    @FXML
    private TextArea messageArea;
    @FXML private Button approveButton;
    @FXML private Button declineButton;
    @FXML private Button closeButton;

    private adoptionRequest currentRequest;
    private adoptionservices adoptionService = new adoptionservices();
    private animalServices animalService = new animalServices();
    private BrevoService brevoService = new BrevoService();

    @FXML
    public void initialize() {
        // Désactiver le scroll automatique du TextArea
        messageArea.setWrapText(true);
        messageArea.setEditable(false);
    }

    public void setRequest(adoptionRequest request) {
        this.currentRequest = request;

        animal a = request.getAnimal();

        // 🐾 Afficher les infos de l'animal
        if (a != null) {
            animalNameLabel.setText("🐾 " + a.getName());
            animalSpeciesLabel.setText("Espèce: " + a.getSpecies());
            animalBreedLabel.setText("Race: " + a.getBreed());
            animalAgeGenderLabel.setText("Âge: " + a.getAge() + " ans | Sexe: " + a.getGender().toString());

            // Charger l'image
            if (a.getImage() != null && !a.getImage().isEmpty()) {
                try {
                    File imageFile = new File("images/" + a.getImage());
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        animalImageView.setImage(image);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement image: " + e.getMessage());
                }
            }
        }

        // 👤 Afficher les infos du demandeur
        if (request.getClient() != null) {
            requesterNameLabel.setText("👤 " + request.getClient().getName());
            requesterEmailLabel.setText("📧 " + request.getClient().getEmail());
            requesterPhoneLabel.setText("☎️ " + request.getClient().getPhone());
        } else {
            requesterNameLabel.setText("👤 Client ID: " + request.getClient_id());
            requesterEmailLabel.setText("📧 -");
            requesterPhoneLabel.setText("☎️ -");
        }

        // 📍 Adresse de la demande
        requesterAddressLabel.setText("📍 " + request.getAddress());

        // 💬 Message du demandeur
        messageArea.setText(request.getMessage().isEmpty() ? "[Aucun message]" : request.getMessage());

        // 📊 Statut actuel
        updateStatusLabel();

        // 🎯 Actions des boutons
        approveButton.setOnAction(e -> approveRequest());
        declineButton.setOnAction(e -> declineRequest());
        closeButton.setOnAction(e -> closeWindow());
    }

    /**
     * ✅ Approuver la demande et envoyer un email
     */
    @FXML
    private void approveRequest() {
        if (currentRequest == null) return;

        // Confirmation
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("✅ Confirmation");
        confirmAlert.setHeaderText("Approuver cette demande ?");
        confirmAlert.setContentText("L'animal sera marqué comme ADOPTÉ et un email sera envoyé au demandeur.");

        if (confirmAlert.showAndWait().orElse(ButtonType.NO) != ButtonType.OK) {
            return;
        }

        try {
            // 1️⃣ Mettre à jour le statut dans la BD
            currentRequest.setStatus(adoptionRequest.status.APPROVED);
            adoptionService.modifier(currentRequest);

            // 2️⃣ Marquer l'animal comme adopté
            if (currentRequest.getAnimal() != null) {
                animal a = currentRequest.getAnimal();
                a.setStatus(animal.status.ADOPTED);
                animalService.modifier(a);
            }

            // 3️⃣ Envoyer un email au demandeur
            System.out.println("📧 Envoi de l'email d'approbation...");
            boolean emailSent = brevoService.sendApprovalEmail(currentRequest);

            if (emailSent) {
                showSuccess("✅ Demande approuvée !",
                        "La demande a été approuvée avec succès.\n" +
                                "Un email de confirmation a été envoyé au demandeur.");
            } else {
                showWarning("⚠️ Approuvée mais email échoué",
                        "La demande a été approuvée mais l'email n'a pas pu être envoyé.\n" +
                                "Vérifiez votre configuration Brevo.");
            }

            updateStatusLabel();
            disableButtons();

        } catch (Exception e) {
            showError("❌ Erreur", "Erreur lors de l'approbation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ❌ Rejeter la demande et envoyer un email
     */
    @FXML
    private void declineRequest() {
        if (currentRequest == null) return;

        // Confirmation
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("❌ Confirmation");
        confirmAlert.setHeaderText("Rejeter cette demande ?");
        confirmAlert.setContentText("Un email sera envoyé au demandeur pour l'informer du rejet.");

        if (confirmAlert.showAndWait().orElse(ButtonType.NO) != ButtonType.OK) {
            return;
        }

        try {
            // 1️⃣ Mettre à jour le statut dans la BD
            currentRequest.setStatus(adoptionRequest.status.REJECTED);
            adoptionService.modifier(currentRequest);

            // 2️⃣ Envoyer un email au demandeur
            System.out.println("📧 Envoi de l'email de rejet...");
            boolean emailSent = brevoService.sendDeclineEmail(currentRequest);

            if (emailSent) {
                showSuccess("❌ Demande rejetée",
                        "La demande a été rejetée avec succès.\n" +
                                "Un email d'explication a été envoyé au demandeur.");
            } else {
                showWarning("⚠️ Rejetée mais email échoué",
                        "La demande a été rejetée mais l'email n'a pas pu être envoyé.\n" +
                                "Vérifiez votre configuration Brevo.");
            }

            updateStatusLabel();
            disableButtons();

        } catch (Exception e) {
            showError("❌ Erreur", "Erreur lors du rejet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 📊 Mettre à jour l'affichage du statut
     */
    private void updateStatusLabel() {
        if (currentRequest == null) return;

        String status = currentRequest.getStatus().toString();
        statusLabel.setText("Statut: " + status);

        switch (currentRequest.getStatus()) {
            case APPROVED:
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                break;
            case REJECTED:
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                break;
            case PENDING:
                statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                break;
        }
    }

    /**
     * 🔒 Désactiver les boutons après une action
     */
    private void disableButtons() {
        approveButton.setDisable(true);
        declineButton.setDisable(true);
    }

    /**
     * ⬅️ Fermer la fenêtre
     */
    @FXML
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // ==================== ALERTES ====================

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}