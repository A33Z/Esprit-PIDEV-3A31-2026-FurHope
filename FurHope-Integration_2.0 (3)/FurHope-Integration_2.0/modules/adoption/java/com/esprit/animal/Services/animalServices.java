package com.esprit.animal.Services;

import com.esprit.animal.entities.Compte;
import com.esprit.animal.entities.User;
import com.esprit.animal.entities.animal;
import com.esprit.animal.utils.MyDataBase;
import com.esprit.animal.utils.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class animalServices implements ICrud<animal> {
    private final Connection con;

    public animalServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(animal pet) throws SQLException {
        int ownerCompteId = pet.getOwnerCompteId() > 0 ? pet.getOwnerCompteId() : Session.getCompteId();
        if (ownerCompteId <= 0) {
            throw new SQLException("Invalid owner compte id.");
        }

        String sql = "INSERT INTO animal(name, species, breed, age, gender, description, status, image, owner_compte_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pet.getName());
            ps.setString(2, pet.getSpecies());
            ps.setString(3, pet.getBreed());
            ps.setInt(4, pet.getAge());
            ps.setString(5, pet.getGender().toString());
            ps.setString(6, pet.getDescription());
            ps.setString(7, pet.getStatus().toString());
            ps.setString(8, pet.getImage());
            ps.setInt(9, ownerCompteId);
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int idAnimal) throws SQLException {
        String sql = "DELETE FROM animal WHERE idAnimal = ?";
        try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
            preparedStatement.setInt(1, idAnimal);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public List<animal> afficher() throws SQLException {
        List<animal> animals = new ArrayList<>();

        String sql = "SELECT a.*, " +
                "c.id_compte AS owner_compte_id, c.user_id AS owner_user_id, c.username AS owner_username, c.role AS owner_role, c.status AS owner_compte_status, " +
                "COALESCE(u.id_user, u.id) AS owner_id_user, u.name AS owner_name, u.email AS owner_email, u.phone AS owner_phone " +
                "FROM animal a " +
                "LEFT JOIN compte c ON a.owner_compte_id = c.id_compte " +
                "LEFT JOIN user u ON (c.user_id = u.id_user OR c.user_id = u.id)";

        try (PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                animal pet = new animal();
                pet.setId(rs.getInt("idAnimal"));
                pet.setName(rs.getString("name"));
                pet.setSpecies(rs.getString("species"));
                pet.setBreed(rs.getString("breed"));
                pet.setAge(rs.getInt("age"));
                pet.setGender(animal.gender.valueOf(rs.getString("gender")));
                pet.setDescription(rs.getString("description"));
                pet.setStatus(animal.status.valueOf(rs.getString("status")));
                pet.setImage(rs.getString("image"));
                pet.setOwnerCompteId(rs.getInt("owner_compte_id"));

                int ownerIdUser = rs.getInt("owner_id_user");
                if (!rs.wasNull() && ownerIdUser > 0) {
                    User ownerUser = new User();
                    ownerUser.setId(ownerIdUser);
                    ownerUser.setName(rs.getString("owner_name"));
                    ownerUser.setEmail(rs.getString("owner_email"));
                    ownerUser.setPhone(rs.getInt("owner_phone"));
                    ownerUser.setRole(rs.getString("owner_role"));

                    Compte ownerCompte = new Compte();
                    ownerCompte.setIdCompte(rs.getInt("owner_compte_id"));
                    ownerCompte.setUserId(rs.getInt("owner_user_id"));
                    ownerCompte.setUsername(rs.getString("owner_username"));
                    ownerCompte.setRole(rs.getString("owner_role"));
                    ownerCompte.setStatus(rs.getString("owner_compte_status"));
                    ownerCompte.setUser(ownerUser);

                    pet.setOwnerCompte(ownerCompte);
                }
                animals.add(pet);
            }
        }

        return animals;
    }

    @Override
    public void modifier(animal pet) throws SQLException {
        String sql = "UPDATE animal SET name = ?, species = ?, breed = ?, age = ?, gender = ?, description = ?, status = ? " +
                "WHERE idAnimal = ?";
        try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
            preparedStatement.setString(1, pet.getName());
            preparedStatement.setString(2, pet.getSpecies());
            preparedStatement.setString(3, pet.getBreed());
            preparedStatement.setInt(4, pet.getAge());
            preparedStatement.setString(5, pet.getGender().toString());
            preparedStatement.setString(6, pet.getDescription());
            preparedStatement.setString(7, pet.getStatus().toString());
            preparedStatement.setInt(8, pet.getId());
            preparedStatement.executeUpdate();
        }
    }
}

