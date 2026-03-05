package controllers;

import com.esprit.services.userservices;
import com.esprit.utils.AuthValidation;
import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class ProfileController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField cityField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label statusLabel;
    @FXML
    private Label displayNameLabel;
    @FXML
    private Label displayEmailLabel;
    @FXML
    private Label editHintLabel;
    @FXML
    private VBox editSection;
    @FXML
    private Button editInfoButton;
    @FXML
    private ImageView avatarImageView;

    private final userservices service = new userservices();
    private User currentUser;
    private boolean editMode = false;
    private String pendingProfileImagePath;

    @FXML
    private void initialize() {
        currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please sign in to edit your profile.");
            return;
        }
        if (avatarImageView != null) {
            avatarImageView.setClip(new Circle(85, 85, 85));
        }
        fillForm(currentUser);
        setEditMode(false);
    }

    private void fillForm(User user) {
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        addressField.setText(user.getAddress());
        cityField.setText(user.getCity());

        String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
        if (fullName.isEmpty()) {
            fullName = "User #" + user.getId();
        }
        if (displayNameLabel != null) {
            displayNameLabel.setText(fullName);
        }
        if (displayEmailLabel != null) {
            displayEmailLabel.setText(safe(user.getEmail()));
        }
        if (statusLabel != null) {
            statusLabel.setText(user.isActive() ? "Active Account" : "Blocked Account");
        }

        pendingProfileImagePath = user.getProfileImagePath();
        applyAvatarImage(pendingProfileImagePath);
    }

    @FXML
    private void toggleEditMode(ActionEvent event) {
        setEditMode(!editMode);
    }

    @FXML
    private void cancelEditMode(ActionEvent event) {
        if (currentUser != null) {
            fillForm(currentUser);
            clearPasswordFields();
        }
        setEditMode(false);
    }

    private void setEditMode(boolean enabled) {
        editMode = enabled;
        if (editSection != null) {
            editSection.setManaged(enabled);
            editSection.setVisible(enabled);
        }
        if (editInfoButton != null) {
            editInfoButton.setText(enabled ? "Close" : "Edit");
        }
        if (editHintLabel != null) {
            editHintLabel.setManaged(!enabled);
            editHintLabel.setVisible(!enabled);
        }
    }

    @FXML
    private void changeAvatarImage(ActionEvent event) {
        File selectedFile = chooseImageFile(event);
        if (selectedFile == null) {
            return;
        }
        try {
            pendingProfileImagePath = copyAvatarToLocalStore(selectedFile);
            applyAvatarImage(pendingProfileImagePath);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Image Error", "Unable to save selected profile image.");
        }
    }

    private File chooseImageFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        return chooser.showOpenDialog(stage);
    }

    @FXML
    private void saveProfile(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please sign in to edit your profile.");
            return;
        }
        if (!validateInputs()) {
            return;
        }

        String newPassword = passwordField.getText().trim();
        String passwordToSave = newPassword.isEmpty() ? currentUser.getPassword() : newPassword;

        User updated = new User(
                currentUser.getId(),
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                emailField.getText().trim(),
                passwordToSave,
                phoneField.getText().trim(),
                addressField.getText().trim(),
                cityField.getText().trim(),
                currentUser.getRole(),
                currentUser.isActive(),
                currentUser.getCreatedAt(),
                pendingProfileImagePath
        );

        try {
            service.modifier(updated);
            SessionContext.setCurrentUser(updated);
            currentUser = updated;
            fillForm(updated);
            clearPasswordFields();
            setEditMode(false);
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Your profile has been updated.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not update profile.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/dashboard.fxml");
    }

    private boolean validateInputs() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || city.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please fill all required fields.");
            return false;
        }
        if (!AuthValidation.isValidName(firstName) || !AuthValidation.isValidName(lastName)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Invalid first name or last name format.");
            return false;
        }
        if (!AuthValidation.isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Invalid email format.");
            return false;
        }
        if (!AuthValidation.isValidPhone(phone)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Phone must be 8 to 15 digits.");
            return false;
        }
        if (!password.isEmpty()) {
            if (!AuthValidation.isStrongPassword(password)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Password must be at least 8 chars with uppercase, lowercase, and number.");
                return false;
            }
            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Password confirmation does not match.");
                return false;
            }
        }

        try {
            if (service.existsByEmailExcludingId(email, currentUser.getId())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Email already exists.");
                return false;
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Unable to validate email uniqueness.");
            return false;
        }

        return true;
    }

    private void clearPasswordFields() {
        passwordField.clear();
        confirmPasswordField.clear();
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Image createGradientImage(int width, int height, Color topColor, Color bottomColor) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            double t = (double) y / Math.max(1, height - 1);
            Color c = topColor.interpolate(bottomColor, t);
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, c);
            }
        }
        return image;
    }

    private void applyAvatarImage(String imagePath) {
        if (avatarImageView == null) {
            return;
        }

        Image image = loadAvatarImage(imagePath);
        if (image == null || image.isError()) {
            image = createGradientImage(180, 180, Color.web("#4a6cae"), Color.web("#7fa6e8"));
        }
        avatarImageView.setImage(image);
    }

    private Image loadAvatarImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        String trimmedPath = imagePath.trim();
        try {
            if (trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://") || trimmedPath.startsWith("file:")) {
                return new Image(trimmedPath, false);
            }
            File file = new File(trimmedPath);
            if (!file.exists()) {
                return null;
            }
            return new Image(file.toURI().toString(), false);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String copyAvatarToLocalStore(File source) throws IOException {
        String ext = extractExtension(source.getName());
        int userId = currentUser == null ? 0 : currentUser.getId();
        Path targetDirectory = Path.of(System.getProperty("user.home"), ".furhope", "avatars");
        Files.createDirectories(targetDirectory);
        Path targetFile = targetDirectory.resolve("user-" + userId + "-" + System.currentTimeMillis() + ext);
        Files.copy(source.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        return targetFile.toAbsolutePath().toString();
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return ".png";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".png";
        }
        String ext = fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp" -> ext;
            default -> ".png";
        };
    }
}
