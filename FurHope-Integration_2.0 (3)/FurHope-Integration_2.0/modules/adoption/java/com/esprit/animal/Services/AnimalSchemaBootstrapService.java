package com.esprit.animal.Services;

import com.esprit.animal.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class AnimalSchemaBootstrapService {

    private AnimalSchemaBootstrapService() {
    }

    public static void ensureSchemaReady() {
        Connection connection = MyDataBase.getInstance().getConnection();
        if (connection == null) {
            return;
        }

        try {
            ensureUserTable(connection);
            ensureCompteTable(connection);
            ensureAnimalTable(connection);
            ensureAdoptionRequestTable(connection);
        } catch (SQLException e) {
            throw new RuntimeException("Animal schema bootstrap failed: " + e.getMessage(), e);
        }
    }

    private static void ensureUserTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "user")) {
            execute(connection,
                    "CREATE TABLE `user` (" +
                            "`id_user` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "`name` VARCHAR(150) NULL," +
                            "`email` VARCHAR(150) NULL," +
                            "`phone` VARCHAR(20) NULL," +
                            "`password` VARCHAR(255) NULL," +
                            "`role` VARCHAR(30) NULL" +
                            ")");
            return;
        }

        if (!columnExists(connection, "user", "id_user")) {
            execute(connection, "ALTER TABLE `user` ADD COLUMN `id_user` INT NULL");
        }
        if (columnExists(connection, "user", "id")) {
            execute(connection, "UPDATE `user` SET `id_user` = `id` WHERE (`id_user` IS NULL OR `id_user` = 0)");
        }

        if (!columnExists(connection, "user", "name")) {
            execute(connection, "ALTER TABLE `user` ADD COLUMN `name` VARCHAR(150) NULL");
        }
        if (columnExists(connection, "user", "first_name") && columnExists(connection, "user", "last_name")) {
            execute(connection,
                    "UPDATE `user` SET `name` = TRIM(CONCAT(COALESCE(`first_name`, ''), ' ', COALESCE(`last_name`, '')))" +
                            " WHERE (`name` IS NULL OR `name` = '')");
        } else if (columnExists(connection, "user", "first_name")) {
            execute(connection, "UPDATE `user` SET `name` = `first_name` WHERE (`name` IS NULL OR `name` = '')");
        }

        if (!columnExists(connection, "user", "email")) {
            execute(connection, "ALTER TABLE `user` ADD COLUMN `email` VARCHAR(150) NULL");
        }
        if (!columnExists(connection, "user", "phone")) {
            execute(connection, "ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) NULL");
        }
        if (!columnExists(connection, "user", "password")) {
            execute(connection, "ALTER TABLE `user` ADD COLUMN `password` VARCHAR(255) NULL");
        }
        if (!columnExists(connection, "user", "role")) {
            execute(connection, "ALTER TABLE `user` ADD COLUMN `role` VARCHAR(30) NULL");
        }
    }

    private static void ensureCompteTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "compte")) {
            execute(connection,
                    "CREATE TABLE `compte` (" +
                            "`id_compte` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "`user_id` INT NOT NULL," +
                            "`username` VARCHAR(100) NOT NULL," +
                            "`password` VARCHAR(255) NOT NULL," +
                            "`role` VARCHAR(30) NOT NULL DEFAULT 'CLIENT'," +
                            "`status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'" +
                            ")");
            return;
        }

        if (!columnExists(connection, "compte", "id_compte")) {
            execute(connection, "ALTER TABLE `compte` ADD COLUMN `id_compte` INT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST");
        }
        if (!columnExists(connection, "compte", "user_id")) {
            execute(connection, "ALTER TABLE `compte` ADD COLUMN `user_id` INT NOT NULL DEFAULT 0");
        }
        if (!columnExists(connection, "compte", "username")) {
            execute(connection, "ALTER TABLE `compte` ADD COLUMN `username` VARCHAR(100) NOT NULL DEFAULT ''");
        }
        if (!columnExists(connection, "compte", "password")) {
            execute(connection, "ALTER TABLE `compte` ADD COLUMN `password` VARCHAR(255) NOT NULL DEFAULT ''");
        }
        if (!columnExists(connection, "compte", "role")) {
            execute(connection, "ALTER TABLE `compte` ADD COLUMN `role` VARCHAR(30) NOT NULL DEFAULT 'CLIENT'");
        }
        if (!columnExists(connection, "compte", "status")) {
            execute(connection, "ALTER TABLE `compte` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
        }
    }

    private static void ensureAnimalTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "animal")) {
            execute(connection,
                    "CREATE TABLE `animal` (" +
                            "`idAnimal` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "`name` VARCHAR(100) NOT NULL," +
                            "`species` VARCHAR(50) NOT NULL," +
                            "`breed` VARCHAR(100) NULL," +
                            "`age` INT NULL," +
                            "`gender` VARCHAR(16) NULL," +
                            "`description` TEXT NULL," +
                            "`status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'," +
                            "`image` VARCHAR(255) NULL," +
                            "`owner_compte_id` INT NULL" +
                            ")");
            return;
        }

        if (!columnExists(connection, "animal", "image")) {
            execute(connection, "ALTER TABLE `animal` ADD COLUMN `image` VARCHAR(255) NULL");
        }
        if (!columnExists(connection, "animal", "owner_compte_id")) {
            execute(connection, "ALTER TABLE `animal` ADD COLUMN `owner_compte_id` INT NULL");
        }
        if (!columnExists(connection, "animal", "status")) {
            execute(connection, "ALTER TABLE `animal` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'");
        }
        if (!columnExists(connection, "animal", "description")) {
            execute(connection, "ALTER TABLE `animal` ADD COLUMN `description` TEXT NULL");
        }

        Integer firstCompteId = firstCompteId(connection);
        if (firstCompteId != null && firstCompteId > 0) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE `animal` SET `owner_compte_id` = ? WHERE (`owner_compte_id` IS NULL OR `owner_compte_id` = 0)")) {
                statement.setInt(1, firstCompteId);
                statement.executeUpdate();
            }
        }
    }

    private static void ensureAdoptionRequestTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "adoptionrequest")) {
            execute(connection,
                    "CREATE TABLE `adoptionrequest` (" +
                            "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "`animal_id` INT NOT NULL," +
                            "`client_compte_id` INT NOT NULL," +
                            "`message` TEXT NULL," +
                            "`phone` VARCHAR(50) NULL," +
                            "`address` VARCHAR(255) NULL," +
                            "`status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                            "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                            ")");
        }

        if (!columnExists(connection, "adoptionrequest", "message")) {
            execute(connection, "ALTER TABLE `adoptionrequest` ADD COLUMN `message` TEXT NULL");
        }
        if (!columnExists(connection, "adoptionrequest", "phone")) {
            execute(connection, "ALTER TABLE `adoptionrequest` ADD COLUMN `phone` VARCHAR(50) NULL");
        }
        if (!columnExists(connection, "adoptionrequest", "address")) {
            execute(connection, "ALTER TABLE `adoptionrequest` ADD COLUMN `address` VARCHAR(255) NULL");
        }
        if (!columnExists(connection, "adoptionrequest", "created_at")) {
            execute(connection,
                    "ALTER TABLE `adoptionrequest` ADD COLUMN `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        }

        if (tableExists(connection, "adoption_request")) {
            execute(connection,
                    "INSERT INTO `adoptionrequest` (`id`, `animal_id`, `client_compte_id`, `status`, `created_at`) " +
                            "SELECT ar.`id`, ar.`animal_id`, ar.`client_id`, ar.`status`, ar.`request_date` " +
                            "FROM `adoption_request` ar " +
                            "ON DUPLICATE KEY UPDATE " +
                            "`animal_id` = VALUES(`animal_id`), " +
                            "`client_compte_id` = VALUES(`client_compte_id`), " +
                            "`status` = VALUES(`status`), " +
                            "`created_at` = VALUES(`created_at`)");
        }
    }

    private static Integer firstCompteId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT `id_compte` FROM `compte` ORDER BY `id_compte` ASC LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                int value = resultSet.getInt(1);
                return resultSet.wasNull() ? null : value;
            }
        }
        return null;
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
