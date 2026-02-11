package com.esprit.entities;

public class User {

    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String city;
    private boolean isVerified;


    public User() {}


    public User(String fullName, String email, String phone,
                String role, String city, boolean isVerified) {

        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.city = city;
        this.isVerified = isVerified;
    }


    public User(int id, String fullName, String email, String phone,
                String role, String city, boolean isVerified) {

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.city = city;
        this.isVerified = isVerified;
    }

    // getters & setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", city='" + city + '\'' +
                ", isVerified=" + isVerified +
                '}';
    }
}
