package com.esprit.animal.Services;

import com.esprit.animal.entities.AnimalStatistics;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.entities.animal;
import com.esprit.animal.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatisticsService {

    private final Connection connection;

    public StatisticsService() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    public AnimalStatistics getAnimalStatistics() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection unavailable.");
        }

        AnimalStatistics stats = new AnimalStatistics();

        stats.setTotalAnimals(countAnimals());
        stats.setAvailableAnimals(countAnimalsByStatus(animal.status.AVAILABLE.name()));
        stats.setAdoptedAnimals(countAnimalsByStatus(animal.status.ADOPTED.name()));

        stats.setTotalRequests(countRequests());
        stats.setPendingRequests(countRequestsByStatus(adoptionRequest.status.PENDING.name()));
        stats.setApprovedRequests(countRequestsByStatus(adoptionRequest.status.APPROVED.name()));
        stats.setRejectedRequests(countRequestsByStatus(adoptionRequest.status.REJECTED.name()));

        String commonSpecies = getMostCommonSpecies();
        stats.setMostCommonSpecies(commonSpecies == null || commonSpecies.isBlank() ? "N/A" : commonSpecies);

        return stats;
    }

    private int countAnimals() throws SQLException {
        String sql = "SELECT COUNT(*) FROM animal";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countAnimalsByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM animal WHERE status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int countRequests() throws SQLException {
        String sql = "SELECT COUNT(*) FROM adoptionrequest";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countRequestsByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM adoptionrequest WHERE status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private String getMostCommonSpecies() throws SQLException {
        String sql = "SELECT species, COUNT(*) AS total " +
                "FROM animal " +
                "WHERE species IS NOT NULL AND TRIM(species) <> '' " +
                "GROUP BY species " +
                "ORDER BY total DESC, species ASC " +
                "LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString("species") : null;
        }
    }
}

