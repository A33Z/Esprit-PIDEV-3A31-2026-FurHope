package com.projet.entities;

public class Panier {

    // attributs
    private int id;
    private int idProduit;
    private String title;
    private double totalP;
    private double totalt;
    private int qty;

    // constructeur vide
    public Panier() {}

    // constructeur complet
    public Panier(int id, int idProduit, String title, double totalP, double totalt, int qty) {
        this.id = id;
        this.idProduit = idProduit;
        this.title = title;
        this.totalP = totalP;
        this.totalt = totalt;
        this.qty = qty;
    }

    // getters & setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(int idProduit) {
        this.idProduit = idProduit;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getTotalP() {
        return totalP;
    }

    public void setTotalP(double totalP) {
        this.totalP = totalP;
    }

    public double getTotalt() {
        return totalt;
    }

    public void setTotalt(double totalt) {
        this.totalt = totalt;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    @Override
    public String toString() {
        return "Panier{" +
                "id=" + id +
                ", idProduit=" + idProduit +
                ", title='" + title + '\'' +
                ", totalP=" + totalP +
                ", totalt=" + totalt +
                ", qty=" + qty +
                '}';
    }
}

