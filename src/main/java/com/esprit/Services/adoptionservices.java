package com.esprit.Services;

import com.esprit.entities.adoptionRequest;
import com.esprit.entities.animal;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class adoptionservices implements ICrud<adoptionRequest>{
    Connection con;

    public adoptionservices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(adoptionRequest adoption) throws SQLException {
        String sql = "INSERT INTO adoptionrequest (animal_id, client_id, message, phone, address, status) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, adoption.getAnimal_id());
        ps.setInt(2, adoption.getClient_id());
        ps.setString(3, adoption.getMessage());
        ps.setString(4, adoption.getPhone());
        ps.setString(5, adoption.getAddress());
        ps.setString(6, adoption.getStatus().toString());
        ps.executeUpdate();

    }

    @Override
    public void supprimer(int id ) throws SQLException {
        String sql = "DELETE FROM `adoptionrequest` WHERE `id`=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("adoption request suupprimer");


    }

    @Override
    public List<adoptionRequest> afficher() throws SQLException {
        List<adoptionRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM adoptionrequest";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            adoptionRequest request = new adoptionRequest();
            request.setId(rs.getInt("id"));
            request.setAnimal_id(rs.getInt("animal_id"));
            request.setClient_id(rs.getInt("client_id"));
request.setMessage(rs.getString("message"));
request.setPhone(rs.getString("phone"));
request.setAddress(rs.getString("address"));
            request.setStatus(adoptionRequest.status.valueOf(rs.getString("status")));


            requests.add(request);

        }
        return requests;

    }

    @Override
    public void modifier(adoptionRequest request) throws SQLException {
        String sql ="UPDATE `adoptionrequest` SET `animal_id`=? ,`client_id`=?, `message`=?,`phone`=?,`address`=?,`status`=? WHERE `id`=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, request.getAnimal_id());
        preparedStatement.setInt(2, request.getClient_id());
        preparedStatement.setString(3, request.getMessage());
        preparedStatement.setString(4, request.getPhone());
        preparedStatement.setString(5, request.getAddress());
        preparedStatement.setString(6, "PENDING");  // fixed value
        preparedStatement.setInt(7, request.getId());

        preparedStatement.executeUpdate();


    }
    public List<adoptionRequest> getRequestsForMyAnimals(int ownerId) throws SQLException {

        List<adoptionRequest> requests = new ArrayList<>();

        String sql = "SELECT r.*, a.name as animalName, a.species, a.breed, a.age, a.gender, a.image " +
                "FROM adoptionrequest r " +
                "JOIN animal a ON r.animal_id = a.idAnimal " +
                "WHERE a.ownerid = ?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, ownerId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            adoptionRequest r = new adoptionRequest();
            r.setId(rs.getInt("id"));
            r.setAnimal_id(rs.getInt("animal_id"));
            r.setClient_id(rs.getInt("client_id"));
            r.setStatus(adoptionRequest.status.valueOf(rs.getString("status")));

            // 🔹 Créer l’animal et l’injecter dans la demande
            animal a = new animal();
            a.setId(rs.getInt("animal_id"));
            a.setName(rs.getString("animalName"));
            a.setSpecies(rs.getString("species"));
            a.setBreed(rs.getString("breed"));
            a.setAge(rs.getInt("age"));
            a.setGender(animal.gender.valueOf(rs.getString("gender")));
            a.setImage(rs.getString("image"));

            r.setAnimal(a); // ⚡ on stocke directement dans l’objet

            requests.add(r);
        }

        return requests;
    }
}
