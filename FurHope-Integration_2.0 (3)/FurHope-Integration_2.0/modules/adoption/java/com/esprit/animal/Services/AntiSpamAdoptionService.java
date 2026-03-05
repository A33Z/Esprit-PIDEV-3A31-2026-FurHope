package com.esprit.animal.Services;

import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.utils.MyDataBase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Local anti-spam validator for adoption requests.
 */
public class AntiSpamAdoptionService {

    private static final int DAILY_LIMIT = 3;
    private final Connection connection;

    private boolean dateColumnResolved;
    private String resolvedDateColumn;

    public AntiSpamAdoptionService() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    public ValidationResult validateRequest(int clientCompteId, int animalId) {
        if (clientCompteId <= 0 || animalId <= 0) {
            return ValidationResult.fail("Invalid request data.");
        }

        if (connection == null) {
            return ValidationResult.fail("Database connection unavailable.");
        }

        try {
            int requestsToday = countRequestsToday(clientCompteId);
            if (requestsToday >= DAILY_LIMIT) {
                return ValidationResult.fail("Daily adoption request limit reached.");
            }

            if (hasPendingRequest(clientCompteId, animalId)) {
                return ValidationResult.fail("You already have a pending request for this animal.");
            }

            if (hasAnyRequestForAnimal(clientCompteId, animalId)) {
                return ValidationResult.fail("You have already sent a request for this animal.");
            }

            return ValidationResult.ok();
        } catch (SQLException e) {
            return ValidationResult.fail("Validation failed: " + e.getMessage());
        }
    }

    private int countRequestsToday(int clientCompteId) throws SQLException {
        String dateColumn = resolveDateColumn();
        String sql;

        if (dateColumn != null) {
            sql = "SELECT COUNT(*) FROM adoptionrequest WHERE client_compte_id = ? AND DATE(`" + dateColumn + "`) = CURDATE()";
        } else {
            // Fallback when request date column is not available in schema.
            sql = "SELECT COUNT(*) FROM adoptionrequest WHERE client_compte_id = ?";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientCompteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private boolean hasAnyRequestForAnimal(int clientCompteId, int animalId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM adoptionrequest WHERE client_compte_id = ? AND animal_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientCompteId);
            ps.setInt(2, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean hasPendingRequest(int clientCompteId, int animalId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM adoptionrequest WHERE client_compte_id = ? AND animal_id = ? AND status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientCompteId);
            ps.setInt(2, animalId);
            ps.setString(3, adoptionRequest.status.PENDING.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private String resolveDateColumn() throws SQLException {
        if (dateColumnResolved) {
            return resolvedDateColumn;
        }

        Set<String> columns = getAdoptionRequestColumns();
        String[] candidates = {
                "created_at",
                "createdat",
                "request_date",
                "created_date",
                "created_on",
                "date_created",
                "created"
        };

        for (String candidate : candidates) {
            if (columns.contains(candidate)) {
                resolvedDateColumn = candidate;
                dateColumnResolved = true;
                return resolvedDateColumn;
            }
        }

        dateColumnResolved = true;
        resolvedDateColumn = null;
        return null;
    }

    private Set<String> getAdoptionRequestColumns() throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();

        for (String tableName : Arrays.asList("adoptionrequest", "adoptionRequest", "ADOPTIONREQUEST")) {
            try (ResultSet rs = metaData.getColumns(catalog, null, tableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName != null) {
                        columns.add(columnName.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return columns;
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}

