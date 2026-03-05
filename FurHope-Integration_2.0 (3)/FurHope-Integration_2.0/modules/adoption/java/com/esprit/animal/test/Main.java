package com.esprit.animal.test;

import com.esprit.animal.Services.adoptionservices;
import com.esprit.animal.Services.animalServices;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.entities.animal;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
     // MyDataBase.getInstance(); connexion de bd
        animalServices ps = new animalServices();
        try {
           //ps.ajouter(new animal("loulou","cat","americain",2,animal.gender.FEMALE,"great cat",  animal.status.AVAILABLE, "C:\\0Users\\joumana\\OneDrive\\Images\\download (1).jpg"));
            //ps.supprimer(3);
            System.out.println(ps.afficher());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        adoptionservices adreq = new adoptionservices();
        try {
            //adreq.ajouter(new adoptionRequest(6,1,"je veux adopter ce chat svp!","5464646587","odsfdjhfejfe",adoptionRequest.status.PENDING));
            //ps.supprimer(3);
            System.out.println(ps.afficher());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
