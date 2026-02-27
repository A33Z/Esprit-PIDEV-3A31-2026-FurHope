package model;

public class Disponibilite {
    public enum Statut {
        VALABLE, NONVALABLE
    }

    private int id_disponibilite;
    private int id; // id du vétérinaire
    private String vetNom; // nom du vétérinaire
    private String starttime;
    private String endtime;
    private Statut statut;

    // Constructeur vide
    public Disponibilite() {}

    // Ancien constructeur (sans vetNom)
    public Disponibilite(int id, String starttime, String endtime, Statut statut) {
        this.id = id;
        this.starttime = starttime;
        this.endtime = endtime;
        this.statut = statut;
    }

    // ✅ Nouveau constructeur avec vetNom (dans le bon ordre)
    public Disponibilite(int id, String vetNom, String starttime, String endtime, Statut statut) {
        this.id = id;
        this.vetNom = vetNom;
        this.starttime = starttime;
        this.endtime = endtime;
        this.statut = statut;
    }

    // ✅ Getters et setters
    public int getId_disponibilite() {
        return id_disponibilite;
    }

    public void setId_disponibilite(int id_disponibilite) {
        this.id_disponibilite = id_disponibilite;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVetNom() {
        return vetNom;
    }

    public void setVetNom(String vetNom) {
        this.vetNom = vetNom;
    }

    public String getStarttime() {
        return starttime;
    }

    public void setStarttime(String starttime) {
        this.starttime = starttime;
    }

    public String getEndtime() {
        return endtime;
    }

    public void setEndtime(String endtime) {
        this.endtime = endtime;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Disponibilite{" +
                "id_disponibilite=" + id_disponibilite +
                ", id=" + id +
                ", vetNom='" + vetNom + '\'' +
                ", starttime='" + starttime + '\'' +
                ", endtime='" + endtime + '\'' +
                ", statut=" + statut +
                '}';
    }
}