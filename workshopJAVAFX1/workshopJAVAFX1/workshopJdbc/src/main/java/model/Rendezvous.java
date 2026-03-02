package model;

public class Rendezvous {

    private int id_rdv;
    private String status;
    private String description;
    private int client_id;
    private int num;
    private int vet_id;
    private int animal_id;
    private int disponibilite_id;
    private String slotStart;
    private String clientNom;
    private String animalNom;
    private String vetNom;

    public Rendezvous() {}

    public Rendezvous(String status, String description, int client_id, int vet_id,
                      int animal_id, int disponibilite_id, String slotStart) {
        this.status = status;
        this.description = description;
        this.client_id = client_id;
        this.vet_id = vet_id;
        this.animal_id = animal_id;
        this.disponibilite_id = disponibilite_id;
        this.slotStart = slotStart;
    }

    public int getId_rdv() { return id_rdv; }
    public void setId_rdv(int id_rdv) { this.id_rdv = id_rdv; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getClient_id() { return client_id; }
    public void setClient_id(int client_id) { this.client_id = client_id; }

    // ✅ Getter/Setter num
    public int getNum() { return num; }
    public void setNum(int num) { this.num = num; }

    public int getVet_id() { return vet_id; }
    public void setVet_id(int vet_id) { this.vet_id = vet_id; }

    public int getAnimal_id() { return animal_id; }
    public void setAnimal_id(int animal_id) { this.animal_id = animal_id; }

    public int getDisponibilite_id() { return disponibilite_id; }
    public void setDisponibilite_id(int disponibilite_id) { this.disponibilite_id = disponibilite_id; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getAnimalNom() { return animalNom; }
    public void setAnimalNom(String animalNom) { this.animalNom = animalNom; }

    public String getVetNom() { return vetNom; }
    public void setVetNom(String vetNom) { this.vetNom = vetNom; }
    public String getSlotStart() { return slotStart; }
    public void setSlotStart(String slotStart) { this.slotStart = slotStart; }

    @Override
    public String toString() {
        return "Rendezvous{id_rdv=" + id_rdv + ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", client_id=" + client_id + ", num=" + num +
                ", vet_id=" + vet_id + ", animal_id=" + animal_id +
                ", slotStart=" + slotStart + ",disponibilite_id=" + disponibilite_id + '}';
    }
}