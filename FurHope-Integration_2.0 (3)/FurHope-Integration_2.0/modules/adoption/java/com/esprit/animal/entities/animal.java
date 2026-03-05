package com.esprit.animal.entities;

import java.util.Objects;

public class animal {

    public enum gender {
        MALE,
        FEMALE
    }

    public enum status {
        AVAILABLE,
        UNAVAILABLE,
        ADOPTED
    }

    private int idAnimal;
    private String name;
    private int age;
    private String species;
    private String breed;
    private String description;
    private gender gender;
    private status status;
    private String image;
    private int ownerCompteId;

    private Compte ownerCompte;

    public animal() {
    }

    public animal(
            String name,
            String species,
            String breed,
            int age,
            gender gender,
            String description,
            status status,
            String image,
            int ownerCompteId
    ) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.description = description;
        this.status = status;
        this.image = image;
        this.ownerCompteId = ownerCompteId;
    }

    public int getId() {
        return idAnimal;
    }

    public void setId(int idAnimal) {
        this.idAnimal = idAnimal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public gender getGender() {
        return gender;
    }

    public void setGender(gender gender) {
        this.gender = gender;
    }

    public status getStatus() {
        return status;
    }

    public void setStatus(status status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getOwnerCompteId() {
        return ownerCompteId;
    }

    public void setOwnerCompteId(int ownerCompteId) {
        this.ownerCompteId = ownerCompteId;
    }

    public Compte getOwnerCompte() {
        return ownerCompte;
    }

    public void setOwnerCompte(Compte ownerCompte) {
        this.ownerCompte = ownerCompte;
    }

    @Override
    public String toString() {
        return "animal{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", species='" + species + '\'' +
                ", breed='" + breed + '\'' +
                ", description='" + description + '\'' +
                ", gender=" + gender +
                ", status=" + status +
                ", image='" + image + '\'' +
                ", ownerCompteId=" + ownerCompteId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        animal animal = (animal) o;
        return age == animal.age
                && Objects.equals(name, animal.name)
                && Objects.equals(species, animal.species)
                && Objects.equals(breed, animal.breed)
                && Objects.equals(description, animal.description)
                && gender == animal.gender
                && status == animal.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, species, breed, description, gender, status);
    }
}

