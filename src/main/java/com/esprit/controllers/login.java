package com.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.esprit.utils.MyDataBase;
import com.esprit.utils.Session;
import com.esprit.utils.ViewNavigator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class login {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void login(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("⚠️ Remplissez tous les champs !");
            return;
        }

        try {
            Connection conn = MyDataBase.getInstance().getConnection();
            String query = "SELECT * FROM user WHERE email = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // ✅ Stocker les infos dans SessionManager
                Session.setUserId(rs.getInt("id"));
                Session.setUserName(rs.getString("name") );
                Session.setUserRole(rs.getString("role"));
                // tu peux ajouter email, phone, etc. si besoin

                // Redirection vers AfficherAnimal (tous les animaux)
                ViewNavigator.goTo(event, "/AfficherAnimal.fxml");
            } else {
                errorLabel.setText("❌ Email ou mot de passe incorrect !");
            }

        } catch (Exception e) {
            errorLabel.setText("Erreur login : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        ViewNavigator.goTo(event, "/Home.fxml");
    }
}