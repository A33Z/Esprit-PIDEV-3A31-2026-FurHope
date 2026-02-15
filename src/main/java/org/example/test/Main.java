package org.example.test;

import org.example.Services.adoptionservices;
import org.example.Services.animalServices;
import org.example.entities.adoptionRequest;
import org.example.entities.animal;

import java.sql.SQLException;
import java.sql.Timestamp;

import static java.sql.Types.NULL;

public class Main {
    public static void main(String[] args) {
     // MyDataBase.getInstance(); connexion de bd
        animalServices ps = new animalServices();
        try {
           // ps.ajouter(new animal("loulou","cat","americain",2,animal.gender.FEMALE,"great cat",  animal.status.AVAILABLE));
            //ps.supprimer(3);
            System.out.println(ps.afficher());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        adoptionservices adreq = new adoptionservices();
        try {
            adreq.ajouter(new adoptionRequest(6,1,"je veux adopter ce chat svp!","5464646587","odsfdjhfejfe",adoptionRequest.status.PENDING));
            //ps.supprimer(3);
            System.out.println(ps.afficher());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}