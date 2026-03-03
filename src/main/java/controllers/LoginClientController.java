package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import utils.MyDatabase;
import utils.SessionManager;
import utils.ViewNavigator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoginClientController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private List<String> emailsList = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT email FROM user WHERE role = 'CLIENT'"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) emailsList.add(rs.getString("email"));
        } catch (Exception e) {
            System.out.println("Erreur chargement emails: " + e.getMessage());
        }

        // Auto-complétion dropdown
        ContextMenu menu = new ContextMenu();
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            menu.hide();
            menu.getItems().clear();
            if (newVal == null || newVal.isEmpty()) return;

            emailsList.stream()
                    .filter(e -> e.toLowerCase().contains(newVal.toLowerCase()))
                    .forEach(email -> {
                        MenuItem item = new MenuItem(email);
                        item.setOnAction(e -> {
                            emailField.setText(email);
                            emailField.positionCaret(email.length());
                            menu.hide();
                        });
                        menu.getItems().add(item);
                    });

            if (!menu.getItems().isEmpty())
                menu.show(emailField,
                        emailField.localToScreen(0, 0).getX(),
                        emailField.localToScreen(0, 0).getY() + emailField.getHeight());
        });
        emailField.focusedProperty().addListener((obs, was, is) -> {
            if (!is) menu.hide();
        });
    }

    @FXML
    private void login(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("⚠️ Veuillez remplir tous les champs !");
            return;
        }

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            String query = "SELECT * FROM user WHERE email = ? AND password = ? AND role = 'CLIENT'";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                SessionManager.setUserId(rs.getInt("id"));
                SessionManager.setUserNom(rs.getString("first_name") + " " + rs.getString("last_name"));
                SessionManager.setUserRole(rs.getString("role"));
                ViewNavigator.goTo(event, "/DashboardClient.fxml", "Mon Espace");
            } else {
                errorLabel.setText("❌ Compte client introuvable !");
            }
        } catch (Exception e) {
            errorLabel.setText("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        ViewNavigator.goTo(event, "/Home.fxml", "Clinique Vétérinaire");
    }
}