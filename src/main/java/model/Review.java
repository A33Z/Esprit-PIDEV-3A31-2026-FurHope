package model;

public class Review {
    private int id;
    private int clientId;
    private int vetId;
    private int rdvId;
    private int rating;
    private String commentaire;

    public Review() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    public int getVetId() { return vetId; }
    public void setVetId(int vetId) { this.vetId = vetId; }
    public int getRdvId() { return rdvId; }
    public void setRdvId(int rdvId) { this.rdvId = rdvId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}