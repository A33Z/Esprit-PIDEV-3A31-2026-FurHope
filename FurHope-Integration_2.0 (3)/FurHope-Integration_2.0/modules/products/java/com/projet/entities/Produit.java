package com.projet.entities;

public class Produit {
    // attributs
    private int id;
    private String title;
    private double price;
    private double tva;
    private String image;
    private String description;
    private int stock;

    // constructeur vide
    public Produit() {}

    // constructeur sans id
    public Produit(String title, double price, double tva, String image, String description, int stock) {
        this.title = title;
        this.price = price;
        this.tva = tva;
        this.image = image;
        this.description = description;
        this.stock = stock;
    }

    // constructeur avec id
    public Produit(int id, String title, double price, double tva, String image, String description, int stock) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.tva = tva;
        this.image = image;
        this.description = description;
        this.stock = stock;
    }

    // getters & setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getTva() {
        return tva;
    }

    public void setTva(double tva) {
        this.tva = tva;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Override
    public String toString() {
        return "Produit{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", tva=" + tva +
                ", image='" + image + '\'' +
                ", description='" + description + '\'' +
                ", stock=" + stock +
                '}';
    }
}

