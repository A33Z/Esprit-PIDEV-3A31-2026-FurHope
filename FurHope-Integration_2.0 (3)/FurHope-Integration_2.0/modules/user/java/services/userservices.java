package com.esprit.services;

import entities.User;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class userservices implements ICrud<User> {

    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean profileSchemaReady;
    private Connection con;

    public userservices() {
        con = MyDataBase.getInstance().getConnection();
        if (con == null) {
            throw new RuntimeException("Database connection unavailable. Check MariaDB credentials and db.* configuration.");
        }
        try {
            ensureProfileImageColumn(con);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify user profile image schema.", e);
        }
    }

    // ===============================
    // ADD USER (Sign Up)
    // ===============================
    @Override
    public void ajouter(User user) throws SQLException {

        String sql = "INSERT INTO user " +
                "(first_name, last_name, email, password, phone, address, city, role, active, profile_image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
        ps.setString(10, user.getProfileImagePath());

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
            user.add(mapUser(rs));
        }

        return user;
    }

    // ===============================
    // UPDATE USER
    // ===============================
    public void modifier(User user) throws SQLException {

        String sql = "UPDATE user SET " +
                "first_name=?, last_name=?, email=?, password=?, phone=?, address=?, city=?, role=?, active=?, profile_image_path=? " +
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
        ps.setString(10, user.getProfileImagePath());
        ps.setInt(11, user.getId());

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

        // Correct login:
        // pending vets can still access regular user pages until approval.
        User user = mapUser(rs);
        if (!user.isActive() && !"VETERINAIRE".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("ACCOUNT_INACTIVE");
        }

        return user;
    }

    public List<User> getPendingVets() throws SQLException {
        String sql = "SELECT * FROM user WHERE role = ? AND active = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, "VETERINAIRE");
        ps.setBoolean(2, false);

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
        u.setProfileImagePath(readOptionalString(rs, "profile_image_path"));

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            u.setCreatedAt(timestamp.toLocalDateTime());
        }

        return u;
    }

    private String readOptionalString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static void ensureProfileImageColumn(Connection connection) throws SQLException {
        synchronized (SCHEMA_LOCK) {
            if (profileSchemaReady) {
                return;
            }
            if (!columnExists(connection, "user", "profile_image_path")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE user ADD COLUMN profile_image_path VARCHAR(1024) NULL");
                }
            }
            profileSchemaReady = true;
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, table, column)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, table.toUpperCase(), column.toUpperCase())) {
            return rs.next();
        }
    }

}
