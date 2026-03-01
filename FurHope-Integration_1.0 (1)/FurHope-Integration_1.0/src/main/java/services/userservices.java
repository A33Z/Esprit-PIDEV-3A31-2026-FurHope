package com.esprit.services;

import entities.User;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class userservices implements ICrud<User> {

    private Connection con;

    public userservices() {
        con = MyDataBase.getInstance().getConnection();
    }

    // ===============================
    // ADD USER (Sign Up)
    // ===============================
    @Override
    public void ajouter(User user) throws SQLException {

        String sql = "INSERT INTO user " +
                "(first_name, last_name, email, password, phone, address, city, role, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, user.getFirstName());
        ps.setString(2, user.getLastName());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getPassword());
        ps.setString(5, user.getPhone());
        ps.setString(6, user.getAddress());
        ps.setString(7, user.getCity());
        ps.setString(8, user.getRole());
        ps.setBoolean(9, user.isActive());

        ps.executeUpdate();

        System.out.println("User added successfully!");
    }

    // ===============================
    // DELETE USER
    // ===============================
    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM user WHERE id = ?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("User deleted successfully!");
    }

    // ===============================
    // DISPLAY USERS
    // ===============================
    @Override
    public List<User> afficher() throws SQLException {

        List<User> user = new ArrayList<>();

        String sql = "SELECT * FROM user";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            User u = new User();

            u.setId(rs.getInt("id"));
            u.setFirstName(rs.getString("first_name"));
            u.setLastName(rs.getString("last_name"));
            u.setEmail(rs.getString("email"));
            u.setPassword(rs.getString("password"));
            u.setPhone(rs.getString("phone"));
            u.setAddress(rs.getString("address"));
            u.setCity(rs.getString("city"));
            u.setRole(rs.getString("role"));
            u.setActive(rs.getBoolean("active"));

            Timestamp timestamp = rs.getTimestamp("created_at");
            if (timestamp != null) {
                u.setCreatedAt(timestamp.toLocalDateTime());
            }

            user.add(u);
        }

        return user;
    }

    // ===============================
    // UPDATE USER
    // ===============================
    public void modifier(User user) throws SQLException {

        String sql = "UPDATE user SET " +
                "first_name=?, last_name=?, email=?, password=?, phone=?, address=?, city=?, role=?, active=? " +
                "WHERE id=?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, user.getFirstName());
        ps.setString(2, user.getLastName());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getPassword());
        ps.setString(5, user.getPhone());
        ps.setString(6, user.getAddress());
        ps.setString(7, user.getCity());
        ps.setString(8, user.getRole());
        ps.setBoolean(9, user.isActive());
        ps.setInt(10, user.getId());

        ps.executeUpdate();

        System.out.println("User updated successfully!");
    }

    // ===============================
    // LOGIN METHOD
    // ===============================
    public User login(String email, String password) throws SQLException {

        String sql = "SELECT * FROM user WHERE email = ?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            // Email does not exist
            throw new RuntimeException("EMAIL_NOT_FOUND");
        }

        String storedPassword = rs.getString("password");

        if (!storedPassword.equals(password)) {
            // Password is wrong
            throw new RuntimeException("WRONG_PASSWORD");
        }

        // Correct login
        User user = mapUser(rs);
        if (!user.isActive()) {
            throw new RuntimeException("ACCOUNT_INACTIVE");
        }

        return user;
    }

    public List<User> getPendingVets() throws SQLException {
        String sql = "SELECT * FROM user WHERE (role = ? OR role = ?) AND active = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, "VETERINAIRE");
        ps.setString(2, "HOTEL_MANAGER");
        ps.setBoolean(3, false);

        ResultSet rs = ps.executeQuery();
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(mapUser(rs));
        }
        return users;
    }

    public void approveUser(int id) throws SQLException {
        String sql = "UPDATE user SET active = ? WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setBoolean(1, true);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    public void setActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE user SET active = ? WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setBoolean(1, active);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM user WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByEmailExcludingId(String email, int userId) throws SQLException {
        String sql = "SELECT 1 FROM user WHERE email = ? AND id <> ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM user WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    public void updatePasswordById(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public OptionalInt findFirstActiveAdminId() throws SQLException {
        String sql = "SELECT id FROM user WHERE UPPER(role) = 'ADMIN' AND active = 1 ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return OptionalInt.of(rs.getInt("id"));
            }
            return OptionalInt.empty();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();

        u.setId(rs.getInt("id"));
        u.setFirstName(rs.getString("first_name"));
        u.setLastName(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setCity(rs.getString("city"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("active"));

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            u.setCreatedAt(timestamp.toLocalDateTime());
        }

        return u;
    }

}
