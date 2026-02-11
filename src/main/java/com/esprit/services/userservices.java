package com.esprit.services;

import com.esprit.entities.User;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class userservices implements ICrud<User> {

    Connection con;

    public userservices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(User user) throws SQLException {

        String sql = "INSERT INTO `user`(`full_name`, `email`, `phone`, `role`, `city`, `is_verified`) " +
                "VALUES ('"+user.getFullName()+"','"+user.getEmail()+"','"+user.getPhone()+"'," +
                "'"+user.getRole()+"','"+user.getCity()+"',"+user.isVerified()+")";

        Statement statement = con.createStatement();
        statement.executeUpdate(sql);

        System.out.println("User ajouté avec succes!");
    }

    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM `user` WHERE `id`=?";

        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();

        System.out.println("User supprimé");
    }

    @Override
    public List<User> afficher() throws SQLException {

        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM user";
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {

            User u = new User();

            u.setId(rs.getInt("id"));
            u.setFullName(rs.getString("full_name"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setRole(rs.getString("role"));
            u.setCity(rs.getString("city"));
            u.setVerified(rs.getBoolean("is_verified"));

            users.add(u);
        }

        return users;
    }



    // ✅ Real update method (same workshop style)
    public void modifier(User user) throws SQLException {

        String sql = "UPDATE `user` SET full_name=?, email=?, phone=?, role=?, city=?, is_verified=? WHERE id=?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, user.getFullName());
        ps.setString(2, user.getEmail());
        ps.setString(3, user.getPhone());
        ps.setString(4, user.getRole());
        ps.setString(5, user.getCity());
        ps.setBoolean(6, user.isVerified());
        ps.setInt(7, user.getId());

        ps.executeUpdate();

        System.out.println("User modifié avec succes!");
    }
}
