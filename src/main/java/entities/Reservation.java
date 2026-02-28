package entities;

import java.sql.Date;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {

    private int id;
    private int clientId;
    private int animalId;
    private int hotelId;
    private Date reservationDate;
    private Date startDate;
    private Date endDate;
    private int guestCount;
    private BigDecimal nightlyRate;
    private BigDecimal totalPrice;
    private ReservationStatus status;

    public Reservation() {}

    public Reservation(int clientId, int animalId, int hotelId,
                       Date startDate, Date endDate, ReservationStatus status) {
        this(
                0,
                clientId,
                animalId,
                hotelId,
                Date.valueOf(LocalDate.now()),
                startDate,
                endDate,
                1,
                defaultNightlyRate(),
                defaultTotalPrice(),
                status
        );
    }

    public Reservation(int clientId, int animalId, int hotelId,
                       Date reservationDate, Date startDate, Date endDate,
                       int guestCount, ReservationStatus status) {
        this(
                0,
                clientId,
                animalId,
                hotelId,
                reservationDate,
                startDate,
                endDate,
                guestCount,
                defaultNightlyRate(),
                defaultTotalPrice(),
                status
        );
    }

    public Reservation(int clientId, int animalId, int hotelId,
                       Date reservationDate, Date startDate, Date endDate,
                       int guestCount, BigDecimal nightlyRate, BigDecimal totalPrice,
                       ReservationStatus status) {
        this(
                0,
                clientId,
                animalId,
                hotelId,
                reservationDate,
                startDate,
                endDate,
                guestCount,
                nightlyRate,
                totalPrice,
                status
        );
    }

    public Reservation(int id, int clientId, int animalId, int hotelId,
                       Date startDate, Date endDate, ReservationStatus status) {
        this(
                id,
                clientId,
                animalId,
                hotelId,
                Date.valueOf(LocalDate.now()),
                startDate,
                endDate,
                1,
                defaultNightlyRate(),
                defaultTotalPrice(),
                status
        );
    }

    public Reservation(int id, int clientId, int animalId, int hotelId,
                       Date reservationDate, Date startDate, Date endDate,
                       int guestCount, ReservationStatus status) {
        this(
                id,
                clientId,
                animalId,
                hotelId,
                reservationDate,
                startDate,
                endDate,
                guestCount,
                defaultNightlyRate(),
                defaultTotalPrice(),
                status
        );
    }

    public Reservation(int id, int clientId, int animalId, int hotelId,
                       Date reservationDate, Date startDate, Date endDate,
                       int guestCount, BigDecimal nightlyRate, BigDecimal totalPrice,
                       ReservationStatus status) {
        this.id = id;
        this.clientId = clientId;
        this.animalId = animalId;
        this.hotelId = hotelId;
        this.reservationDate = reservationDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.guestCount = guestCount;
        this.nightlyRate = nightlyRate;
        this.totalPrice = totalPrice;
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

    public Date getReservationDate() { return reservationDate; }
    public void setReservationDate(Date reservationDate) { this.reservationDate = reservationDate; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }

    public BigDecimal getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    private static BigDecimal defaultNightlyRate() {
        return new BigDecimal("85.00");
    }

    private static BigDecimal defaultTotalPrice() {
        return new BigDecimal("85.00");
    }
}

