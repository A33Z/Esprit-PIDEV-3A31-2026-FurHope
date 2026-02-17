package org.example.furhopeuser;

import com.esprit.entities.User;
import com.esprit.services.userservices;

import java.sql.SQLException;

public class FurhopeUserApplication {

    public static void main(String[] args) {

        userservices service = new userservices();

        try {

            // Create new user
            User u = new User(
                    "Hamza",
                    "Ben Yahia",
                    "hamza@test.com",
                    "123456",        // password (temporary plain text)
                    "22123456",
                    "Rue de Paris",
                    "Tunis",
                    "CLIENT"
            );

            service.ajouter(u);

            System.out.println("---- LIST USERS ----");

            for (User user : service.afficher()) {
                System.out.println(user);
            }

            // Update first user (if exists)
            if (!service.afficher().isEmpty()) {

                User toUpdate = service.afficher().get(0);

                toUpdate.setCity("Ariana");
                toUpdate.setPhone("99999999");

                service.modifier(toUpdate);

                System.out.println("User updated successfully!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
