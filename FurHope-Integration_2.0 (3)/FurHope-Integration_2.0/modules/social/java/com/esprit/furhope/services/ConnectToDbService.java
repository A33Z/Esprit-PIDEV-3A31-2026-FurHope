package com.esprit.furhope.services;

import com.esprit.utils.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ConnectToDbService {

    private static final Object CONNECTION_LOCK = new Object();
    private static volatile Connection sharedConnection;

    protected final Connection con;

    protected ConnectToDbService() {
        this.con = getSharedConnection();
    }

    protected static Connection getSharedConnection() {
        try {
            synchronized (CONNECTION_LOCK) {
                if (sharedConnection == null || sharedConnection.isClosed() || !sharedConnection.isValid(2)) {
                    sharedConnection = DBConnection.getConnection();
                    SocialSchemaService.ensureSocialSchema(sharedConnection);
                }
                return sharedConnection;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize social database connection: " + e.getMessage(), e);
        }
    }

    public static boolean isConnectionHealthy() {
        try {
            Connection connection = getSharedConnection();
            if (connection == null || connection.isClosed()) {
                return false;
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT 1")) {
                return rs.next();
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
