package com.esprit.entities;

public class adoptionRequest {

    public enum status {
        PENDING,
        APPROVED,
        REJECTED
    }

    private int id;
    private int animal_id;
    private int client_id;
    private String message;
    private String phone;
    private String address;
    private status status;


    public adoptionRequest() {
    }

    public adoptionRequest(int animal_id, int client_id, String message, String phone, String address, status status) {
        this.animal_id = animal_id;
        this.client_id = client_id;
        this.message = message;
        this.phone = phone;
        this.address = address;
        this.status = status;

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAnimal_id() {
        return animal_id;
    }

    public void setAnimal_id(int animal_id) {
        this.animal_id = animal_id;
    }

    public int getClient_id() {
        return client_id;
    }

    public void setClient_id(int client_id) {
        this.client_id = client_id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public status getStatus() {
        return status;
    }

    public void setStatus(status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "adoptionRequest{" +
                "animal_id=" + animal_id +
                ", client_id=" + client_id +
                ", message='" + message + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", status=" + status +
                '}';
    }
}