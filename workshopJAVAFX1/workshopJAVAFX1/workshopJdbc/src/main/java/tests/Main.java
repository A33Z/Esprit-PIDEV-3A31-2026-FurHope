package tests;

import model.Disponibilite;
import services.IService;
import services.ServiceDisponibilite;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        IService<Disponibilite> disponibiliteIService = new ServiceDisponibilite();

        try {
            // ✅ LocalDateTime au lieu de String
            LocalDateTime start = LocalDateTime.of(2026, 2, 20, 10, 20);
            LocalDateTime end   = LocalDateTime.of(2026, 2, 20, 14, 30);

            disponibiliteIService.add(new Disponibilite(1, start, end, Disponibilite.Statut.VALABLE));
            System.out.println("✅ Disponibilité ajoutée !");

        } catch (SQLException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }
}