

    package com.esprit.controllers;

import com.esprit.Services.animalServices;
import com.esprit.entities.animal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

    public class myanimals {

        @FXML
        private GridPane myanimalsGrid;

        private animalServices service = new animalServices();

        @FXML
        public void initialize() {
            loadAnimals();
        }

        private void loadAnimals() {
            try {
                myanimalsGrid.getChildren().clear(); // avoid duplicates

                List<animal> animals = service.afficher();

                int column = 0;
                int row = 0;

                for (animal a : animals) {

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AnimalCard.fxml"));
                    Node card = loader.load(); // ✅ FIXED

                    AnimalCard controller = loader.getController();
                    controller.setData(a);

                    myanimalsGrid.add(card, column, row);

                    column++;
                    if (column == 3) {
                        column = 0;
                        row++;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

