package com.esprit.controllers;

import com.esprit.entities.User;
import com.esprit.services.auth.PasswordResetService;
import com.esprit.services.auth.SendGridEmailSender;
import com.esprit.services.auth.TwilioSmsSender;
import com.esprit.services.userservices;
import com.esprit.utils.AuthValidation;
import com.esprit.utils.PhoneUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField codeField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private RadioButton methodSmsRadio;

    @FXML
    private RadioButton methodEmailRadio;

    private final userservices userService = new userservices();
    private final PasswordResetService passwordResetService = new PasswordResetService();

    @FXML
    private void sendCode(ActionEvent event) {
        String email = sanitize(emailField.getText());
        if (!AuthValidation.isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter a valid email.");
            return;
        }

        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                showAlert(Alert.AlertType.INFORMATION, "Request Accepted",
                        "If this email exists, a reset code has been sent.");
                return;
            }

            if (methodEmailRadio != null && methodEmailRadio.isSelected()) {
                sendCodeByEmail(email);
            } else {
                sendCodeBySms(email, user);
            }
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.WARNING, "Please Wait", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to send reset code.");
        }
    }

    private void sendCodeBySms(String email, User user) {
        if (!TwilioSmsSender.isConfigured()) {
            showAlert(Alert.AlertType.ERROR, "Twilio Not Configured",
                    "Twilio credentials are missing. Configure TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM_NUMBER.");
            return;
        }

        String normalizedPhone = PhoneUtils.normalizeForSms(user.getPhone());
        if (!PhoneUtils.isLikelyE164(normalizedPhone)) {
            showAlert(Alert.AlertType.WARNING, "Phone Invalid",
                    "This account has an invalid phone number for SMS reset.");
            return;
        }

        passwordResetService.sendOtpBySms(email, normalizedPhone);
        showAlert(Alert.AlertType.INFORMATION, "Code Sent",
                "A reset code was sent to " + maskPhone(normalizedPhone) + ".");
    }

    private void sendCodeByEmail(String email) {
        if (!SendGridEmailSender.isConfigured()) {
            showAlert(Alert.AlertType.ERROR, "Email API Not Configured",
                    "Email API credentials are missing. Configure BREVO_API_KEY and MAIL_FROM_ADDRESS.");
            return;
        }

        try {
            passwordResetService.sendOtpByEmail(email);
            showAlert(Alert.AlertType.INFORMATION, "Code Sent",
                    "A reset code was sent to " + maskEmail(email) + ".");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            String details = cause == null ? e.getMessage() : cause.getMessage();
            showAlert(Alert.AlertType.ERROR, "Email Delivery Failed",
                    details == null || details.isBlank() ? "Unable to deliver email reset code." : details);
        }
    }

    @FXML
    private void chooseSms(ActionEvent event) {
        if (methodSmsRadio != null) {
            methodSmsRadio.setSelected(true);
        }
        if (methodEmailRadio != null) {
            methodEmailRadio.setSelected(false);
        }
    }

    @FXML
    private void chooseEmail(ActionEvent event) {
        if (methodEmailRadio != null) {
            methodEmailRadio.setSelected(true);
        }
        if (methodSmsRadio != null) {
            methodSmsRadio.setSelected(false);
        }
    }

    @FXML
    private void initialize() {
        if (methodSmsRadio != null && methodEmailRadio != null) {
            methodSmsRadio.setSelected(true);
            methodEmailRadio.setSelected(false);
        }
    }

    @FXML
    private void resetPassword(ActionEvent event) {
        String email = sanitize(emailField.getText());
        String code = sanitize(codeField.getText());
        String newPassword = sanitize(newPasswordField.getText());
        String confirmPassword = sanitize(confirmPasswordField.getText());

        if (!AuthValidation.isValidEmail(email) || code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please fill all fields correctly.");
            return;
        }
        if (!AuthValidation.isStrongPassword(newPassword)) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Password must be at least 8 chars with uppercase, lowercase, and number.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Password confirmation does not match.");
            return;
        }

        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                showAlert(Alert.AlertType.WARNING, "Invalid Request", "Invalid code or expired request.");
                return;
            }

            boolean validCode = passwordResetService.verifyOtp(email, code);
            if (!validCode) {
                showAlert(Alert.AlertType.WARNING, "Invalid Code", "Invalid or expired code.");
                return;
            }

            userService.updatePasswordById(user.getId(), newPassword);
            passwordResetService.clear(email);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Password updated successfully.");
            goBack(event);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to reset password.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        switchScene(event, "/signin.fxml");
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

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
