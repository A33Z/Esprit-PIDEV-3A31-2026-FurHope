package com.esprit.furhope.services;

import com.esprit.utils.DBConnection;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.sql.Connection;
import java.sql.SQLException;

public final class DbHealthCheckService {

    private DbHealthCheckService() {
    }

    public static boolean verifyConnectionAtStartup() {
        try (Connection ignored = DBConnection.getConnection()) {
            return true;
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Connection Error");
            alert.setHeaderText("Unable to connect to the local database");
            alert.setContentText(
                    "Please start MariaDB/XAMPP and verify db.host/db.port/db.name/db.user/db.password settings.\n\n" +
                            "Technical details: " + e.getMessage()
            );
            alert.getButtonTypes().setAll(ButtonType.OK);
            alert.showAndWait();
            return false;
        }
    }
}
