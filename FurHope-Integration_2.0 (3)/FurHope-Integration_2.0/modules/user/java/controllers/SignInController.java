package controllers;

import entities.User;
import integrations.auth.FaceAuthService;
import com.esprit.services.userservices;
import com.esprit.utils.AuthValidation;
import com.esprit.utils.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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

    private userservices service;
    private final FaceAuthService faceAuthService = new FaceAuthService();

    @FXML
    private void initialize() {
        try {
            service = new userservices();
        } catch (RuntimeException e) {
            service = null;
            setFormError("Database connection unavailable. Please start MariaDB and verify DB settings.");
        }
        Platform.runLater(this::syncThemeToggleIcon);
    }

    @FXML
    private void login(ActionEvent event) {

        clearErrors();
        if (!validateCredentialsInputsInline()) {
            return;
        }
        if (service == null) {
            setFormError("Login service unavailable. Verify database connection.");
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
    private void loginWithGoogle(ActionEvent event) {
        setFormError("Google sign-in is not configured in this build.");
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
        if (service == null) {
            setFormError("Login service unavailable. Verify database connection.");
            return;
        }

        try {
            User user = service.findByEmail(email);
            if (user == null) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
                return;
            }
            if (!faceAuthService.hasEnrollment(email)) {
                setFormError("No face enrolled for this email. Use Enroll Face first.");
                return;
            }
            Optional<BufferedImage> captured = captureFaceFromCamera(event, "Face Login", email);
            if (captured.isEmpty()) {
                return;
            }
            if (!user.isActive() && !"VETERINAIRE".equalsIgnoreCase(user.getRole())) {
                setFormError("Account awaiting approval.");
                return;
            }

            SessionContext.setCurrentUser(user);
            loadAccueil(event);
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Face login failed: " + e.getMessage());
        }
    }

    @FXML
    private void enrollFace(ActionEvent event) {
        clearErrors();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            setFieldError(emailField, emailErrorLabel, "Email is required to enroll face.");
            return;
        }
        if (!AuthValidation.isValidEmail(email)) {
            setFieldError(emailField, emailErrorLabel, "Invalid email format.");
            return;
        }
        if (service == null) {
            setFormError("Login service unavailable. Verify database connection.");
            return;
        }

        try {
            if (!service.existsByEmail(email)) {
                setFieldError(emailField, emailErrorLabel, "Email not found.");
                return;
            }
            Optional<BufferedImage> captured = captureFaceFromCamera(event, "Face Enrollment", null);
            if (captured.isEmpty()) {
                return;
            }
            faceAuthService.enroll(email, captured.get());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Face enrolled successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            setFormError("Face enrollment failed: " + e.getMessage());
        }
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

    private Optional<BufferedImage> captureFaceFromCamera(ActionEvent event, String title, String autoVerifyEmail) {
        FaceAuthService.CameraSession session = faceAuthService.openCameraSession();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        boolean autoVerify = autoVerifyEmail != null && !autoVerifyEmail.isBlank();
        dialog.setHeaderText(autoVerify
                ? "Center your face in the frame. Login happens automatically when matched."
                : "Center your face in the frame, then click Use Frame.");

        Stage owner = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        dialog.initOwner(owner);

        ButtonType useFrameType = new ButtonType("Use Frame", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        if (autoVerify) {
            dialog.getDialogPane().getButtonTypes().add(cancelType);
        } else {
            dialog.getDialogPane().getButtonTypes().addAll(useFrameType, cancelType);
        }

        ImageView preview = new ImageView();
        preview.setFitWidth(900);
        preview.setFitHeight(560);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setStyle("-fx-border-color: #90A4AE; -fx-border-width: 3; -fx-border-radius: 10;");

        Label statusLabel = new Label("Starting camera...");
        VBox content = new VBox(10, preview, statusLabel);
        dialog.getDialogPane().setPrefSize(980, 700);
        dialog.getDialogPane().setMinSize(920, 660);
        dialog.getDialogPane().setContent(content);

        Button useFrameButton = autoVerify ? null : (Button) dialog.getDialogPane().lookupButton(useFrameType);
        if (useFrameButton != null) {
            useFrameButton.setDisable(true);
        }

        AtomicReference<BufferedImage> latestFrame = new AtomicReference<>();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicBoolean verified = new AtomicBoolean(false);
        AtomicBoolean verifying = new AtomicBoolean(false);
        AtomicLong firstDetectedAtMs = new AtomicLong(0L);
        AtomicLong lastVerifyAtMs = new AtomicLong(0L);
        ScheduledExecutorService previewWorker = Executors.newSingleThreadScheduledExecutor();
        previewWorker.scheduleAtFixedRate(() -> {
            if (!running.get()) {
                return;
            }
            try {
                FaceAuthService.PreviewResult result = faceAuthService.capturePreview(session);
                latestFrame.set(result.getRawImage());
                Platform.runLater(() -> {
                    preview.setImage(toFxImage(result.getImage()));
                    String status = result.getMessage();
                    if (autoVerify && result.isDetected() && !verified.get()) {
                        long seenAt = firstDetectedAtMs.get();
                        long now = System.currentTimeMillis();
                        long elapsed = seenAt == 0L ? 0L : (now - seenAt);
                        if (elapsed < 1500L) {
                            long remainingSec = (long) Math.ceil((1500L - elapsed) / 1000.0);
                            status = "Face detected. Scanning... " + remainingSec + "s";
                        } else {
                            status = "Face detected. Verifying...";
                        }
                    }
                    statusLabel.setText(status);
                    if (useFrameButton != null) {
                        useFrameButton.setDisable(!result.isDetected());
                    }
                });
                if (autoVerify && !verified.get()) {
                    if (!result.isDetected()) {
                        firstDetectedAtMs.set(0L);
                        Platform.runLater(() ->
                                preview.setStyle("-fx-border-color: #90A4AE; -fx-border-width: 3; -fx-border-radius: 10;")
                        );
                        return;
                    }

                    long now = System.currentTimeMillis();
                    long seenAt = firstDetectedAtMs.get();
                    if (seenAt == 0L) {
                        firstDetectedAtMs.compareAndSet(0L, now);
                        seenAt = firstDetectedAtMs.get();
                    }

                    Platform.runLater(() ->
                            preview.setStyle("-fx-border-color: #1E88E5; -fx-border-width: 4; -fx-border-radius: 10;")
                    );

                    if (now - seenAt < 1500L) {
                        return;
                    }

                    long lastAttempt = lastVerifyAtMs.get();
                    if (now - lastAttempt < 500L || !lastVerifyAtMs.compareAndSet(lastAttempt, now)) {
                        return;
                    }

                    if (!verifying.compareAndSet(false, true)) {
                        return;
                    }

                    boolean match = false;
                    try {
                        match = faceAuthService.verify(autoVerifyEmail, result.getRawImage());
                    } catch (Exception ignored) {
                    }
                    if (match) {
                        verified.set(true);
                        running.set(false);
                        Platform.runLater(() -> {
                            preview.setStyle("-fx-border-color: #00C853; -fx-border-width: 5; -fx-border-radius: 10;");
                            statusLabel.setText("Face verified. Logging you in...");
                        });
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        Platform.runLater(() -> {
                            dialog.setResult(ButtonType.OK);
                            dialog.close();
                        });
                    } else {
                        Platform.runLater(() -> statusLabel.setText("Face detected but not matched. Keep your face centered."));
                    }
                    verifying.set(false);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Camera error: " + ex.getMessage());
                    if (useFrameButton != null) {
                        useFrameButton.setDisable(true);
                    }
                });
            }
        }, 0, 180, TimeUnit.MILLISECONDS);

        Optional<ButtonType> chosen;
        try {
            chosen = dialog.showAndWait();
        } finally {
            running.set(false);
            previewWorker.shutdownNow();
            session.close();
        }

        if (autoVerify && verified.get()) {
            return Optional.ofNullable(latestFrame.get());
        }
        if (!autoVerify && chosen.isPresent() && chosen.get() == useFrameType) {
            BufferedImage frame = latestFrame.get();
            if (frame == null) {
                setFormError("No frame captured. Please retry.");
                return Optional.empty();
            }
            return Optional.of(frame);
        }
        return Optional.empty();
    }

    private Image toFxImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        int[] argb = new int[width * height];
        source.getRGB(0, 0, width, height, argb, 0, width);
        writer.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), argb, 0, width);
        return image;
    }
}
