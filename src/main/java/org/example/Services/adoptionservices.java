package org.example.Services;

import org.example.entities.adoptionRequest;
import org.example.entities.adoptionRequest;
import org.example.utils.MyDataBase;

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
        String sql = "INSERT INTO `adoption_request`( `animal_id`,`client_id`, `message`,`phone`,`address`) VALUES ("+adoption.getAnimal_id()+","+adoption.getClient_id()+" ,'"+adoption.getMessage()+"','"+adoption.getPhone()+"','"+adoption.getAddress()+"')";
        Statement statement = con.createStatement();
        statement.executeUpdate(sql);
        System.out.println("adoption request ajoutée avec succes!");


    }

    @Override
    public void supprimer(int id ) throws SQLException {
        String sql = "DELETE FROM `adoptionRequest` WHERE `idadoptionRequest`=?";
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("adoption request suupprimer");


    }

    @Override
    public List<adoptionRequest> afficher() throws SQLException {
        List<adoptionRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM adoption_request";
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
    public void modifier(int idadoptionRequest) throws SQLException {
        String sql ="UPDATE `adoptionRequest` SET `idadoptionRequest`='[value-1]',`name`='[value-2]',`species`='[value-3]',`breed`='[value-4]',`age`='[value-5]',`gender`='[value-6]',`description`='[value-7]',`status`='[value-8]' WHERE 1";


    }
}
