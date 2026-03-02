package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDatabase {

    // ✅ URL corrigée avec zeroDateTimeBehavior
    final String URL = "jdbc:mysql://localhost:3306/furhope2?serverTimezone=UTC&zeroDateTimeBehavior=CONVERT_TO_NULL";
    final String SERVER_URL = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
    final String DB_NAME = "furhope2"; // ✅ même nom que l'URL

    final String USER = "root";
    final String PASSWORD = "";

    private Connection connection;
    private String connectionError;

    private static MyDatabase instance;

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            initializeSchema(connection);
            System.out.println("Connected");
        } catch (SQLException e) {
            if (isUnknownDatabaseError(e)) {
                try {
                    createDatabaseIfMissing();
                    connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    initializeSchema(connection);
                    System.out.println("Connected (database created automatically)");
                    return;
                } catch (SQLException ex) {
                    connectionError = ex.getMessage();
                    System.err.println("Database initialization failed: " + connectionError);
                }
            } else {
                connectionError = e.getMessage();
                System.err.println("Database connection failed: " + connectionError);
            }
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null)
            instance = new MyDatabase();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public Connection getConnectionOrThrow() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String message = "Connexion DB indisponible (URL: " + URL + ").";
            if (connectionError != null && !connectionError.isBlank()) {
                message += " Cause: " + connectionError;
            }
            throw new SQLException(message);
        }
        return connection;
    }

    private boolean isUnknownDatabaseError(SQLException e) {
        return e.getMessage() != null && e.getMessage().toLowerCase().contains("unknown database");
    }

    private void createDatabaseIfMissing() throws SQLException {
        try (Connection serverConnection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement statement = serverConnection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }
    }

    private void initializeSchema(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {

            // ✅ DATETIME au lieu de VARCHAR pour starttime/endtime
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS disponibilite (" +
                            "id_disponibilite INT PRIMARY KEY AUTO_INCREMENT, " +
                            "id INT NOT NULL, " +           // ✅ id au lieu de vet_id
                            "starttime DATETIME NOT NULL, " + // ✅ DATETIME
                            "endtime DATETIME NOT NULL, " +   // ✅ DATETIME
                            "statut VARCHAR(20) NOT NULL" +
                            ")"
            );

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS rendezvous (" +
                            "id_rdv INT PRIMARY KEY AUTO_INCREMENT, " +
                            "status VARCHAR(30) NOT NULL, " +
                            "description VARCHAR(500) NOT NULL, " +
                            "client_id INT NOT NULL, " +
                            "vet_id INT NOT NULL, " +
                            "animal_id INT NOT NULL, " +
                            "disponibilite_id INT NOT NULL, " +
                            "CONSTRAINT fk_rdv_dispo FOREIGN KEY (disponibilite_id) " +
                            "REFERENCES disponibilite(id_disponibilite) ON DELETE RESTRICT" +
                            ")"
            );
        }
    }
}