package org.example;

import entities.Hotel;
import entities.Reservation;
import services.HotelService;
import services.ReservationService;

import java.util.List;

public class MainHR {

    public static void main(String[] args) {

        System.out.println("Connecting to database...\n");

        HotelService hotelService = new HotelService();
        ReservationService reservationService = new ReservationService();

        // 🔹 Retrieve Hotels
        System.out.println("=== HOTELS IN DATABASE ===");

        List<Hotel> hotels = hotelService.getAllHotels();

        if (hotels.isEmpty()) {
            System.out.println("No hotels found.");
        } else {
            for (Hotel h : hotels) {
                System.out.println(
                        "ID: " + h.getId() +
                                " | Name: " + h.getName() +
                                " | Address: " + h.getAddress() +
                                " | Capacity: " + h.getCapacity()
                );
            }
        }

        // 🔹 Retrieve Reservations
        System.out.println("\n=== RESERVATIONS IN DATABASE ===");

        List<Reservation> reservations = reservationService.getAllReservations();

        if (reservations.isEmpty()) {
            System.out.println("No reservations found.");
        } else {
            for (Reservation r : reservations) {
                System.out.println(
                        "ID: " + r.getId() +
                                " | Client: " + r.getClientId() +
                                " | Hotel: " + r.getHotelId() +
                                " | Status: " + r.getStatus()
                );
            }
        }

        System.out.println("\nDone.");
    }
}
