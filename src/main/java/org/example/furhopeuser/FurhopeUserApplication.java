package org.example.furhopeuser;

import com.esprit.entities.User;
import com.esprit.services.userservices;

import java.sql.SQLException;

public class FurhopeUserApplication {

    public static void main(String[] args) {

        userservices service = new userservices();

        try {

            User u = new User(
                    "Hamza Ben Yahia",
                    "hamza@test.com",
                    "22123456",
                    "CLIENT",
                    "Tunis",
                    false
            );

            service.ajouter(u);

            System.out.println("---- LIST USERS ----");
            for (User user : service.afficher()) {
                System.out.println(user);
            }


            User toUpdate = service.afficher().get(0); // first user
            toUpdate.setCity("Ariana");
            toUpdate.setVerified(true);

            service.modifier(toUpdate);



        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
