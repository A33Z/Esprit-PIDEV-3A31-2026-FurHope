package model;

public class Disponibilite {
    public enum Statut {
        VALABLE , NONVALABLE
    }
    private int id_disponibilite , id ;
    private String starttime , endtime ;
    private Statut statut;

    public Disponibilite(){}

    public Disponibilite(int id , String starttime, String endtime,Statut statut) {
        this.id = id;
        this.starttime = starttime;
        this.endtime = endtime;
        this.statut = statut ;
    }
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
                "id='" + id + '\'' +
                ", starttime='" + starttime + '\'' +
                ", endtime=" + endtime +
                ", statut=" + statut +
                '}';
    }
}

