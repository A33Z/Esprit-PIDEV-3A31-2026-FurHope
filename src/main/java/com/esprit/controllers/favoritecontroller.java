package com.esprit.controllers;

import com.esprit.entities.animal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class favoritecontroller {

    @FXML
    private GridPane favoritesGrid;

    @FXML
    public void initialize() {
        loadFavorites();
    }

     public void loadFavorites() {
        favoritesGrid.getChildren().clear();

        List<animal> favorites = AnimalCard.getFavoriteAnimals(); // static list in AnimalCard

        int col = 0;
        int row = 0;

        for (int i = 0; i < favorites.size(); i++) {
            animal a = favorites.get(i);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AnimalCard.fxml"));
                Parent card = loader.load();

                AnimalCard controller = loader.getController();
                controller.setData(a);

                // pass this controller to the card
                controller.setParentController(this);

                favoritesGrid.add(card, col, row);

                col++;
                if (col == 3) { // 3 cards per row
                    col = 0;
                    row++;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}