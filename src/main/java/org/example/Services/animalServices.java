package org.example.Services;

import org.example.entities.animal;
import org.example.utils.MyDataBase;

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
        String sql = "INSERT INTO `animal`( `name`, `species`,`breed`,`age`,`gender`,`description`,`status`) VALUES ('"+Animal.getName()+"','"+Animal.getSpecies()+"','"+Animal.getBreed()+"',"+Animal.getAge()+",'"+Animal.getGender()+"','"+Animal.getDescription()+"','"+Animal.getStatus()+"')";
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

            animals.add(Animal);

        }
        return animals;



    }

    @Override
    public void modifier(int idAnimal) throws SQLException {
        String sql ="UPDATE `animal` SET `idAnimal`='[value-1]',`name`='[value-2]',`species`='[value-3]',`breed`='[value-4]',`age`='[value-5]',`gender`='[value-6]',`description`='[value-7]',`status`='[value-8]' WHERE 1";


    }
}

