package com.esprit.animal.controllers;

import com.esprit.animal.utils.MyDataBase;
import com.esprit.animal.utils.Session;
import com.esprit.animal.utils.ViewNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class login extends BaseUIController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @Override
    protected String getViewPath() {
        return "/animal/login.fxml";
    }

    @Override
    protected String getBackViewPath() {
        return "/animal/Home.fxml";
    }

    @FXML
    private void login(ActionEvent event) {
        String identifier = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Fill all fields.");
            return;
        }

        try {
            Connection conn = MyDataBase.getInstance().getConnection();

            // New auth model: compte is the principal identity.
            String compteQuery = "SELECT c.id_compte, c.role, u.id_user, u.name, u.email, u.phone " +
                    "FROM compte c " +
                    "JOIN user u ON c.user_id = u.id_user " +
                    "WHERE (c.username = ? OR u.email = ?) AND c.password = ? " +
                    "LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(compteQuery)) {
                ps.setString(1, identifier);
                ps.setString(2, identifier);
                ps.setString(3, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Session.setCompteId(rs.getInt("id_compte"));
                        Session.setUserId(rs.getInt("id_user"));
                        Session.setUserName(rs.getString("name"));
                        Session.setUserEmail(rs.getString("email"));
                        Session.setUserPhone(rs.getInt("phone"));
                        Session.setUserRole(rs.getString("role"));
                        ViewNavigator.goTo(event, "/animal/AfficherAnimal.fxml");
                        return;
                    }
                }
            }

            // Fallback for legacy schemas where user table still authenticates directly.
            String[] legacyQueries = {
                    "SELECT id_user, name, email, phone, role FROM user WHERE email = ? AND password = ? LIMIT 1",
                    "SELECT id AS id_user, name, email, phone, role FROM user WHERE email = ? AND password = ? LIMIT 1"
            };
            for (String legacyQuery : legacyQueries) {
                try (PreparedStatement ps = conn.prepareStatement(legacyQuery)) {
                    ps.setString(1, identifier);
                    ps.setString(2, password);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Session.setUserId(rs.getInt("id_user"));
                            Session.setUserName(rs.getString("name"));
                            Session.setUserEmail(rs.getString("email"));
                            Session.setUserPhone(rs.getInt("phone"));
                            Session.setUserRole(rs.getString("role"));
                            ViewNavigator.goTo(event, "/animal/AfficherAnimal.fxml");
                            return;
                        }
                    }
                } catch (Exception ignored) {
                    // Ignore legacy schema mismatch and continue.
                }
            }

            errorLabel.setText("Invalid credentials.");
        } catch (Exception e) {
            errorLabel.setText("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

