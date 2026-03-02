package com.esprit.Services;

import com.esprit.entities.User;
import com.esprit.entities.animal;
import com.esprit.utils.MyDataBase;
import com.esprit.utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class animalServices implements ICrud<animal> {
    Connection con;

    public animalServices() {
        con = MyDataBase.getInstance().getConnection();
    }
    @Override
    public void ajouter(animal Animal) throws SQLException {
        int ownerId = Session.getUserId();
        String sql = "INSERT INTO animal(name, species, breed, age, gender, description, status, image, ownerid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, Animal.getName());
        ps.setString(2, Animal.getSpecies());
        ps.setString(3, Animal.getBreed());
        ps.setInt(4, Animal.getAge());
        ps.setString(5, Animal.getGender().toString()); // MALE ou FEMALE
        ps.setString(6, Animal.getDescription());
        ps.setString(7, Animal.getStatus().toString()); // AVAILABLE, ADOPTED, etc.
        ps.setString(8, Animal.getImage());
        ps.setInt(9, ownerId);

        ps.executeUpdate();
        System.out.println("Animal ajouté avec succès !");
    }

    @Override
    public void supprimer(int idAnimal) throws SQLException {
        String sql = "DELETE FROM `animal` WHERE `idAnimal`=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, idAnimal);
        preparedStatement.executeUpdate();
        System.out.println("animal suupprimer");


    }

    @Override
    public List<animal> afficher() throws SQLException {
        List<animal> animals = new ArrayList<>();

        String sql = "SELECT a.*, u.name as ownerName, u.email as ownerEmail, u.phone as ownerPhone, u.role as ownerRole " +
                "FROM animal a " +
                "JOIN user u ON a.ownerid = u.id";

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            animal a = new animal();
            a.setId(rs.getInt("idAnimal"));
            a.setName(rs.getString("name"));
            a.setSpecies(rs.getString("species"));
            a.setBreed(rs.getString("breed"));
            a.setAge(rs.getInt("age"));
            a.setGender(animal.gender.valueOf(rs.getString("gender")));
            a.setDescription(rs.getString("description"));
            a.setStatus(animal.status.valueOf(rs.getString("status")));
            a.setImage(rs.getString("image"));
            a.setOwnerid(rs.getInt("ownerid"));

            // 🔹 Créer l'objet User pour le propriétaire
            User owner = new User();
            owner.setId(rs.getInt("ownerid"));
            owner.setName(rs.getString("ownerName"));
            owner.setEmail(rs.getString("ownerEmail"));
            owner.setPhone(rs.getInt("ownerPhone"));
            owner.setRole(rs.getString("ownerRole"));

            // 🔹 Associer le propriétaire à l'animal
            a.setOwner(owner);

            animals.add(a);
        }

        return animals;
    }

    @Override
    public void modifier(animal animal) throws SQLException {
        String sql ="UPDATE `animal` SET `name`=? ,`species`=?,`breed`=?,`age`=?,`gender`=?,`description`=?,`status`=?  WHERE `idAnimal`=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1, animal.getName());
        preparedStatement.setString(2, animal.getSpecies());
        preparedStatement.setString(3, animal.getBreed());
        preparedStatement.setInt(4, animal.getAge());
        preparedStatement.setString(5, animal.getGender().toString());
        preparedStatement.setString(6, animal.getDescription());
        preparedStatement.setString(7, animal.getStatus().toString());
        preparedStatement.setInt(8, animal.getId());


        preparedStatement.executeUpdate();


    }
}

