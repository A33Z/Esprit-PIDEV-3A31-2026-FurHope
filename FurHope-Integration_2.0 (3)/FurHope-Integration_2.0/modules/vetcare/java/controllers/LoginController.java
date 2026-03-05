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

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private List<String> emailsList = new ArrayList<>();
    private ContextMenu suggestionsMenu = new ContextMenu();

    @FXML
    public void initialize() {

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT email FROM user WHERE role = 'VETERINAIRE'"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                emailsList.add(rs.getString("email"));
            }
        } catch (Exception e) {
            System.out.println("Erreur chargement emails: " + e.getMessage());
        }


        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            suggestionsMenu.hide();
            suggestionsMenu.getItems().clear();

            if (newVal == null || newVal.trim().isEmpty()) return;

            List<String> filtered = emailsList.stream()
                    .filter(e -> e.toLowerCase().contains(newVal.toLowerCase()))
                    .toList();

            if (filtered.isEmpty()) return;

            for (String email : filtered) {
                MenuItem item = new MenuItem(email);

                item.setOnAction(e -> {
                    emailField.setText(email);
                    emailField.positionCaret(email.length());
                    suggestionsMenu.hide();
                });
                suggestionsMenu.getItems().add(item);
            }


            suggestionsMenu.show(emailField,
                    emailField.localToScreen(0, 0).getX(),
                    emailField.localToScreen(0, 0).getY() + emailField.getHeight()
            );
        });


        emailField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) suggestionsMenu.hide();
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
            String query = "SELECT * FROM user WHERE email = ? AND password = ? AND role = 'VETERINAIRE'";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                SessionManager.setUserId(rs.getInt("id"));
                SessionManager.setUserNom(rs.getString("first_name") + " " + rs.getString("last_name"));
                SessionManager.setUserRole(rs.getString("role"));
                ViewNavigator.goTo(event, "/Dashboard.fxml", "Dashboard Vétérinaire");
            } else {
                errorLabel.setText("❌ Compte vétérinaire introuvable !");
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