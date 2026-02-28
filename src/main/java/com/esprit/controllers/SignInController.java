package com.esprit.controllers;

import com.esprit.entities.User;
import com.esprit.services.userservices;
import com.esprit.services.auth.FaceAuthService;
import com.esprit.services.auth.GoogleAuthService;
import com.github.sarxos.webcam.Webcam;
import com.esprit.utils.AuthValidation;
import com.esprit.utils.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SignInController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Label formErrorLabel;

    @FXML
    private Button themeToggleButton;
    @FXML
    private Button googleButton;

    private final userservices service = new userservices();
    private FaceAuthService faceAuthService;
    private GoogleAuthService googleAuthService;

    @FXML
    private void initialize() {
        googleAuthService = new GoogleAuthService();
        Platform.runLater(this::syncThemeToggleIcon);
    }

    @FXML
    private void login(ActionEvent event) {

        clearErrors();
        if (!validateCredentialsInputsInline()) {
            return;
        }

        try {
            User user = service.login(
                    emailField.getText().trim(),
                    passwordField.getText().trim()
            );

            System.out.println("[LOGIN] id=" + user.getId()
                    + " email=" + user.getEmail()
                    + " firstName=" + user.getFirstName());

            SessionContext.setCurrentUser(user);
            loadAccueil(event);

        } catch (RuntimeException e) {

            if ("EMAIL_NOT_FOUND".equals(e.getMessage())) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
            } else if ("WRONG_PASSWORD".equals(e.getMessage())) {
                setFieldError(passwordField, passwordErrorLabel, "Incorrect password.");
            } else if ("ACCOUNT_INACTIVE".equals(e.getMessage())) {
                setFormError("Account awaiting approval.");
            } else {
                e.printStackTrace();
                setFormError("Login failed: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            setFormError("System error. Please try again.");
        }
    }

    @FXML
    private void enrollFace(ActionEvent event) {
        clearErrors();
        if (!validateCredentialsInputsInline()) {
            return;
        }

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        try {
            faceAuthService();
        } catch (RuntimeException ex) {
            setFormError("Face recognition is unavailable: " + ex.getMessage());
            return;
        }

        setFormError("Opening camera for face enrollment...");

        Task<Void> enrollTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                User user = service.login(email, password);
                BufferedImage captured = captureGuidedFrame("Face Enrollment", 10);
                faceAuthService.enroll(email, captured);
                SessionContext.setCurrentUser(user);
                return null;
            }
        };

        enrollTask.setOnSucceeded(e -> {
            if (formErrorLabel != null) {
                formErrorLabel.setManaged(true);
                formErrorLabel.setVisible(true);
                formErrorLabel.setText("Face enrolled successfully. You can now use Face Login.");
            }
        });

        enrollTask.setOnFailed(e -> {
            Throwable ex = enrollTask.getException();
            String message = ex == null ? "Face enrollment failed." : ex.getMessage();
            if ("EMAIL_NOT_FOUND".equals(message)) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
            } else if ("WRONG_PASSWORD".equals(message)) {
                setFieldError(passwordField, passwordErrorLabel, "Incorrect password.");
            } else if ("ACCOUNT_INACTIVE".equals(message)) {
                setFormError("Account awaiting approval.");
            } else {
                setFormError(message == null ? "Face enrollment failed." : message);
            }
        });

        Thread worker = new Thread(enrollTask, "face-enroll-task");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void loginWithFace(ActionEvent event) {
        clearErrors();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            setFieldError(emailField, emailErrorLabel, "Email is required for face login.");
            return;
        }
        if (!AuthValidation.isValidEmail(email)) {
            setFieldError(emailField, emailErrorLabel, "Invalid email format.");
            return;
        }

        try {
            faceAuthService();
        } catch (RuntimeException ex) {
            setFormError("Face recognition is unavailable: " + ex.getMessage());
            return;
        }

        if (!faceAuthService.hasEnrollment(email)) {
            setFormError("No enrolled face found for this email. Enroll first.");
            return;
        }

        setFormError("Opening camera for face verification...");

        Task<User> verifyTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                User user = service.findByEmail(email);
                if (user == null) {
                    throw new RuntimeException("EMAIL_NOT_FOUND");
                }
                if (!user.isActive()) {
                    throw new RuntimeException("ACCOUNT_INACTIVE");
                }

                BufferedImage captured = captureGuidedFrame("Face Verification", 10);
                boolean matched = faceAuthService.verify(email, captured);
                if (!matched) {
                    throw new RuntimeException("FACE_MISMATCH");
                }
                return user;
            }
        };

        verifyTask.setOnSucceeded(e -> {
            try {
                SessionContext.setCurrentUser(verifyTask.getValue());
                loadAccueilFromNode(emailField);
            } catch (Exception ex) {
                ex.printStackTrace();
                setFormError("Unable to open home page.");
            }
        });

        verifyTask.setOnFailed(e -> {
            Throwable ex = verifyTask.getException();
            String message = ex == null ? "Face login failed." : ex.getMessage();
            if ("EMAIL_NOT_FOUND".equals(message)) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
            } else if ("ACCOUNT_INACTIVE".equals(message)) {
                setFormError("Account awaiting approval.");
            } else if ("FACE_MISMATCH".equals(message)) {
                setFormError("Face not recognized. Try again in good lighting.");
            } else {
                setFormError(message == null ? "Face login failed." : message);
            }
        });

        Thread worker = new Thread(verifyTask, "face-login-task");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void previewFace(ActionEvent event) {
        clearErrors();

        try {
            faceAuthService();
        } catch (RuntimeException ex) {
            setFormError("Face recognition is unavailable: " + ex.getMessage());
            return;
        }

        setFormError("Opening camera for preview...");
        Task<FaceAuthService.PreviewResult> previewTask = new Task<>() {
            @Override
            protected FaceAuthService.PreviewResult call() {
                return faceAuthService.capturePreview();
            }
        };

        previewTask.setOnSucceeded(e -> {
            FaceAuthService.PreviewResult result = previewTask.getValue();
            showFacePreviewDialog(result);
            setFormError(result.getMessage());
        });

        previewTask.setOnFailed(e -> {
            Throwable ex = previewTask.getException();
            setFormError(ex == null ? "Face preview failed." : ex.getMessage());
        });

        Thread worker = new Thread(previewTask, "face-preview-task");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void loginWithGoogle(ActionEvent event) {
        clearErrors();

        try {
            if (!googleAuthService.isConfigured()) {
                setFormError("Google login not configured. Set GOOGLE_OAUTH_CLIENT_ID + GOOGLE_OAUTH_CLIENT_SECRET (or google.secrets.properties).");
                return;
            }

            setFormError("Opening browser for Google authentication...");
            GoogleAuthService.GoogleProfile profile = googleAuthService.authenticate();

            User user = service.findByEmail(profile.email());
            if (user == null) {
                User created = new User(
                        profile.givenName(),
                        profile.familyName(),
                        profile.email(),
                        "google-" + UUID.randomUUID(),
                        "00000000",
                        "Google Account",
                        "N/A",
                        "CLIENT"
                );
                created.setActive(true);
                service.ajouter(created);
                user = service.findByEmail(profile.email());
            }

            if (user == null) {
                setFormError("Google login succeeded but local account could not be loaded.");
                return;
            }
            if (!user.isActive()) {
                setFormError("Account awaiting approval.");
                return;
            }

            SessionContext.setCurrentUser(user);
            loadAccueilFromNode(emailField);
        } catch (Exception e) {
            setFormError(e.getMessage() == null ? "Google login failed." : e.getMessage());
        }
    }

    private FaceAuthService faceAuthService() {
        if (faceAuthService == null) {
            faceAuthService = new FaceAuthService();
        }
        return faceAuthService;
    }

    private boolean validateCredentialsInputsInline() {
        boolean ok = true;

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (email.isEmpty()) {
            setFieldError(emailField, emailErrorLabel, "Email is required.");
            ok = false;
        }

        if (!email.isEmpty() && !AuthValidation.isValidEmail(email)) {
            setFieldError(emailField, emailErrorLabel, "Invalid email format.");
            ok = false;
        }

        if (password.isEmpty()) {
            setFieldError(passwordField, passwordErrorLabel, "Password is required.");
            ok = false;
        }

        return ok;
    }

    private void loadAccueil(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/accueil.fxml"));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                .getScene().getWindow();
        Scene newScene = new Scene(root);
        ThemeManager.applyToScene(newScene);
        stage.setScene(newScene);
        stage.show();
    }

    private void loadAccueilFromNode(Node node) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/accueil.fxml"));
        Stage stage = (Stage) node.getScene().getWindow();
        Scene newScene = new Scene(root);
        ThemeManager.applyToScene(newScene);
        stage.setScene(newScene);
        stage.show();
    }

    @FXML
    private void goBack(ActionEvent event) {

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Welcome.fxml")
            );

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();

            Scene newScene = new Scene(root);
            ThemeManager.applyToScene(newScene);
            stage.setScene(newScene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToForgotPassword(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/forgot_password.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene newScene = new Scene(root);
            ThemeManager.applyToScene(newScene);
            stage.setScene(newScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Unable to open password recovery page.");
        }
    }

    @FXML
    private void toggleDarkMode(ActionEvent event) {
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        ThemeManager.toggle(scene);
        syncThemeToggleIcon();
    }

    @FXML
    private void goToSignUp(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/signup.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource())
                    .getScene().getWindow();
            Scene newScene = new Scene(root);
            ThemeManager.applyToScene(newScene);
            stage.setScene(newScene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Unable to open sign up page.");
        }
    }

    private void syncThemeToggleIcon() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.isDarkModeEnabled() ? "\uD83C\uDF19" : "\u2600");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    private void showFacePreviewDialog(FaceAuthService.PreviewResult result) {
        ImageView imageView = new ImageView(SwingFXUtils.toFXImage(result.getImage(), null));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(420);
        imageView.setSmooth(true);

        Label status = new Label(result.getMessage());
        VBox content = new VBox(10, imageView, status);
        content.setPadding(new Insets(8));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Face Preview");
        alert.setHeaderText("Camera Capture");
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private BufferedImage captureGuidedFrame(String title, int seconds) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<BufferedImage> capturedRef = new AtomicReference<>();
        AtomicReference<RuntimeException> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            List<Webcam> webcams = Webcam.getWebcams();
            if (webcams == null || webcams.isEmpty()) {
                errorRef.set(new RuntimeException("No webcam found."));
                latch.countDown();
                return;
            }

            Webcam webcam = webcams.get(0);
            try {
                webcam.open();
            } catch (RuntimeException ex) {
                errorRef.set(new RuntimeException("Unable to open webcam: " + webcam.getName(), ex));
                latch.countDown();
                return;
            }

            ImageView imageView = new ImageView();
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            Label countdownLabel = new Label("Auto capture in " + seconds + "s");
            Label helpLabel = new Label("Center your face and keep still.");
            VBox footer = new VBox(6, countdownLabel, helpLabel);

            BorderPane content = new BorderPane();
            content.setPadding(new Insets(10));
            content.setCenter(imageView);
            content.setBottom(footer);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            Scene scene = new Scene(content, 1280, 720);
            stage.setScene(scene);
            stage.setMaximized(true);

            imageView.fitWidthProperty().bind(scene.widthProperty().subtract(40));
            imageView.fitHeightProperty().bind(scene.heightProperty().subtract(140));

            AtomicBoolean finished = new AtomicBoolean(false);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            long start = System.currentTimeMillis();
            AtomicReference<BufferedImage> lastFrame = new AtomicReference<>();

            Runnable finishCapture = () -> {
                if (!finished.compareAndSet(false, true)) {
                    return;
                }
                scheduler.shutdownNow();
                BufferedImage finalFrame = lastFrame.get();
                if (finalFrame != null) {
                    capturedRef.set(finalFrame);
                } else {
                    errorRef.set(new RuntimeException("No frame captured from webcam."));
                }
                if (webcam.isOpen()) {
                    webcam.close();
                }
                stage.close();
                latch.countDown();
            };

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        lastFrame.set(frame);
                        Platform.runLater(() -> imageView.setImage(SwingFXUtils.toFXImage(frame, null)));
                    }

                    long elapsedMs = System.currentTimeMillis() - start;
                    long remaining = Math.max(0, seconds - (elapsedMs / 1000));
                    Platform.runLater(() -> countdownLabel.setText("Auto capture in " + remaining + "s"));

                    if (elapsedMs >= (seconds * 1000L)) {
                        Platform.runLater(finishCapture);
                    }
                } catch (RuntimeException ex) {
                    errorRef.set(new RuntimeException("Webcam read failed: " + ex.getMessage(), ex));
                    Platform.runLater(finishCapture);
                }
            }, 0, 120, TimeUnit.MILLISECONDS);

            stage.setOnCloseRequest(e -> finishCapture.run());
            stage.show();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Camera capture interrupted.", e);
        }

        RuntimeException error = errorRef.get();
        if (error != null) {
            throw error;
        }

        BufferedImage image = capturedRef.get();
        if (image == null) {
            throw new RuntimeException("No camera image captured.");
        }
        return image;
    }

    private void clearErrors() {
        clearFieldError(emailField, emailErrorLabel);
        clearFieldError(passwordField, passwordErrorLabel);
        if (formErrorLabel != null) {
            formErrorLabel.setManaged(false);
            formErrorLabel.setVisible(false);
            formErrorLabel.setText("");
        }
    }

    private void setFormError(String message) {
        if (formErrorLabel == null) {
            showAlert(Alert.AlertType.ERROR, "Error", message);
            return;
        }
        formErrorLabel.setText(message);
        formErrorLabel.setManaged(true);
        formErrorLabel.setVisible(true);
    }

    private void setFieldError(TextField field, Label errorLabel, String message) {
        if (field != null) {
            if (!field.getStyleClass().contains("invalid")) {
                field.getStyleClass().add("invalid");
            }
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
            errorLabel.setVisible(true);
        } else {
            setFormError(message);
        }
    }

    private void setFieldError(PasswordField field, Label errorLabel, String message) {
        if (field != null) {
            if (!field.getStyleClass().contains("invalid")) {
                field.getStyleClass().add("invalid");
            }
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setManaged(true);
            errorLabel.setVisible(true);
        } else {
            setFormError(message);
        }
    }

    private void clearFieldError(TextField field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("invalid");
        }
        if (errorLabel != null) {
            errorLabel.setManaged(false);
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }
    }

    private void clearFieldError(PasswordField field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("invalid");
        }
        if (errorLabel != null) {
            errorLabel.setManaged(false);
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }
    }
}
