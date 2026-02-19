package tests;

import model.Disponibilite;
import services.IService;
import services.ServiceDisponibilite;
import utils.MyDatabase;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {

        IService<Disponibilite> disponibiliteIService = new ServiceDisponibilite();

        try {
            disponibiliteIService.add(new Disponibilite(1, "10:20","14:30", Disponibilite.Statut.VALABLE));
            System.out.println("disponibilite ajoutée");
        }catch (SQLException e){

            System.out.println(e.getMessage());
        }

    }
}
