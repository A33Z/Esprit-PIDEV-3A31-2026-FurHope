package com.esprit.Services;

import com.esprit.entities.animal;
import com.esprit.utils.MyDataBase;

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
        String sql = "INSERT INTO `animal`( `name`, `species`,`breed`,`age`,`gender`,`description`,`status`,`image`) VALUES " +
                "('"+Animal.getName()+"','"+Animal.getSpecies()+"','"+Animal.getBreed()+"',"+Animal.getAge()+",'"+Animal.getGender()+"','"+Animal.getDescription()+"','"+Animal.getStatus()+"','"+Animal.getImage()+"')";
        Statement statement = con.createStatement();
        statement.executeUpdate(sql);
        System.out.println("animal ajoutée avec succes!");


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
        String sql = "SELECT * FROM animal";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            animal Animal = new animal();
            Animal.setId(rs.getInt("idAnimal"));
            Animal.setName(rs.getString("name"));
            Animal.setSpecies(rs.getString("Species"));
            Animal.setBreed(rs.getString("Breed"));
            Animal.setAge(rs.getInt("age"));
            Animal.setGender(animal.gender.valueOf(rs.getString("gender")));
            Animal.setDescription(rs.getString("description"));
            Animal.setStatus(animal.status.valueOf(rs.getString("status")));
            Animal.setImage(rs.getString("image"));

            animals.add(Animal);

        }
        return animals;



    }

    @Override
    public void modifier(animal animal) throws SQLException {
        String sql ="UPDATE `animal` SET `name`=? ,`species`=?,`breed`=?,`age`=?,`gender`=?,`description`=?,`status`=? WHERE `idAnimal`=?";
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

