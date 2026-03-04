package com.esprit.entities;

public class adoptionRequest {

    public enum status {
        PENDING,
        APPROVED,
        REJECTED
    }

    private int id;
    private int animal_id;
    private int clientCompteId;
    private String message;
    private String phone;
    private String address;
    private status status;

    private animal animal;
    private Compte clientCompte;

    public adoptionRequest() {
    }

    public adoptionRequest(int animal_id, int clientCompteId, String message, String phone, String address, status status) {
        this.animal_id = animal_id;
        this.clientCompteId = clientCompteId;
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

    public int getClientCompteId() {
        return clientCompteId;
    }

    public void setClientCompteId(int clientCompteId) {
        this.clientCompteId = clientCompteId;
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

    public animal getAnimal() {
        return animal;
    }

    public void setAnimal(animal animal) {
        this.animal = animal;
    }

    public Compte getClientCompte() {
        return clientCompte;
    }

    public void setClientCompte(Compte clientCompte) {
        this.clientCompte = clientCompte;
    }

    @Override
    public String toString() {
        return "adoptionRequest{" +
                "animal_id=" + animal_id +
                ", clientCompteId=" + clientCompteId +
                ", message='" + message + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", status=" + status +
                '}';
    }
}
