package entities;

import java.sql.Date;

public class Reservation {

    private int id;
    private int clientId;
    private int animalId;
    private int hotelId;
    private Date startDate;
    private Date endDate;
    private ReservationStatus status;

    public Reservation() {}

    public Reservation(int clientId, int animalId, int hotelId,
                       Date startDate, Date endDate, ReservationStatus status) {
        this.clientId = clientId;
        this.animalId = animalId;
        this.hotelId = hotelId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Reservation(int id, int clientId, int animalId, int hotelId,
                       Date startDate, Date endDate, ReservationStatus status) {
        this.id = id;
        this.clientId = clientId;
        this.animalId = animalId;
        this.hotelId = hotelId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getAnimalId() { return animalId; }
    public void setAnimalId(int animalId) { this.animalId = animalId; }

    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
}

