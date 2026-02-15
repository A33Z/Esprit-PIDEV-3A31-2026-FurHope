package org.example.entities;

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


    public animal() {}


    public animal( String name, String species,  String breed, int age,  gender gender, String description, status status) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.description = description;
        this.status = status;
    }

    public int getId() {return idAnimal;}

    public void setId(int idAnimal) { this.idAnimal = idAnimal; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public int getAge() { return age;}

    public void setAge(int age) { this.age = age;}

    public String getSpecies() { return species; }

    public void setSpecies(String species) { this.species = species;}

    public String getBreed() { return breed;}

    public void setBreed(String breed) { this.breed = breed; }

    public String getDescription() {return description;}

    public void setDescription(String description) {this.description = description;}

    public gender getGender() {return gender;}

    public void setGender(gender gender) {this.gender = gender;}

    public status getStatus() {return status;}

    public void setStatus(status status) {this.status = status;}

    @Override
    public String toString() {
        return "animal{" +
                "idAnimal=" + idAnimal +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", species='" + species + '\'' +
                ", breed='" + breed + '\'' +
                ", description='" + description + '\'' +
                ", gender=" + gender +
                ", status=" + status +
                '}';
    }
}
