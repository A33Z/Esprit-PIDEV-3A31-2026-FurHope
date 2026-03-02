package model;

import java.time.LocalDateTime;
public class Disponibilite {
    public enum Statut {
        VALABLE, NONVALABLE
    }

    private int id_disponibilite;
    private int id;
    private LocalDateTime starttime ;
    private LocalDateTime endtime ;
    private Statut statut;

    public Disponibilite() {}

    public Disponibilite(int id, LocalDateTime starttime,   LocalDateTime endtime, Statut statut) {
        this.id = id;
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


    public LocalDateTime getStarttime() {
        return starttime;
    }

    public void setStarttime(LocalDateTime starttime) {
        this.starttime = starttime;
    }

    public LocalDateTime getEndtime() {
        return endtime;
    }

    public void setEndtime(LocalDateTime endtime) {
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
                ", starttime='" + starttime + '\'' +
                ", endtime='" + endtime + '\'' +
                ", statut=" + statut +
                '}';
    }
}