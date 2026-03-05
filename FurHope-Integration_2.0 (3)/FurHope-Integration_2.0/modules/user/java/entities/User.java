package entities;

import java.time.LocalDateTime;

public class User {

    private int id;

    private String firstName;
    private String lastName;

    private String email;
    private String password;

    private String phone;
    private String address;
    private String city;

    private String role;
    private String managerId;
    private String profileImagePath;

    private boolean active;

    private LocalDateTime createdAt;

    // Empty constructor
    public User() {}

    // Constructor for registration (no id yet)
    public User(String firstName, String lastName, String email,
                String password, String phone,
                String address, String city, String role) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.role = role;
        this.active = true;
    }

    // Full constructor
    public User(int id, String firstName, String lastName, String email,
                String password, String phone,
                String address, String city,
                String role, boolean active,
                LocalDateTime createdAt) {
        this(id, firstName, lastName, email, password, phone, address, city, role, active, createdAt, null);
    }

    public User(int id, String firstName, String lastName, String email,
                String password, String phone,
                String address, String city,
                String role, boolean active,
                LocalDateTime createdAt, String profileImagePath) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.profileImagePath = profileImagePath;
    }

    public User(int id, String displayName, Role role) {
        this(id, displayName, role, null);
    }

    public User(int id, String displayName, Role role, String managerId) {
        this.id = id;
        this.firstName = displayName;
        this.lastName = "";
        this.role = role == null ? null : role.name();
        this.managerId = managerId;
        this.active = true;
    }

    // =====================
    // Getters & Setters
    // =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    public String getDisplayName() {
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        if (email != null && !email.isBlank()) {
            return email;
        }
        if (managerId != null && !managerId.isBlank()) {
            return managerId;
        }
        return "User #" + id;
    }

    public boolean hasRole(Role expectedRole) {
        return expectedRole != null && role != null && expectedRole.name().equalsIgnoreCase(role);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return getDisplayName() + " (" + role + ")";
    }
}
