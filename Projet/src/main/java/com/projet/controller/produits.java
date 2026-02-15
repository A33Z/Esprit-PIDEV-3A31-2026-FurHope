package com.projet.controller;

import com.projet.entities.Produit;
import com.projet.services.ProduitService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class produits {

    @FXML
    private FlowPane productGrid;

    private final ProduitService ps = new ProduitService();

    @FXML
    public void initialize() {
        loadProducts();
    }

    // Load products from DB
    void loadProducts() {
        try {
            productGrid.getChildren().clear();

            for (Produit p : ps.afficher()) {
                productGrid.getChildren().add(createCard(p));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create product card UI
    StackPane createCard(Produit p) {

        StackPane card = new StackPane();
        card.getStyleClass().add("product-card");
        card.setPrefSize(180, 220);

        // FRONT
        VBox front = new VBox(8);
        front.setAlignment(Pos.CENTER);

        ImageView img = new ImageView();
        img.setFitWidth(140);
        img.setFitHeight(120);
        img.setPreserveRatio(true);

        // safe image loading
        if (p.getImage() != null && !p.getImage().isEmpty()) {
            try {
                img.setImage(new Image("file:" + p.getImage()));
            } catch (Exception e) {
                System.out.println("Image failed: " + p.getImage());
            }
        }

        Label name = new Label(p.getTitle());
        name.getStyleClass().add("product-title");

        front.getChildren().addAll(img, name);

        // BACK
        VBox back = new VBox(8);
        back.setAlignment(Pos.CENTER);

        Label price = new Label("Prix: " + p.getPrice());
        Label stock = new Label("Stock: " + p.getStock());

        back.getChildren().addAll(price, stock);
        back.setVisible(false);

        card.getChildren().addAll(front, back);

        // hover flip
        card.setOnMouseEntered(e -> {
            front.setVisible(false);
            back.setVisible(true);
        });

        card.setOnMouseExited(e -> {
            back.setVisible(false);
            front.setVisible(true);
        });

        return card;
    }

    // Go to add product screen
    @FXML
    void addproduit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterProduit.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Placeholder buttons
    @FXML
    void goHome() {
        System.out.println("Retour accueil");
    }

    @FXML
    void filter() {
        System.out.println("Filtres à venir");
    }
}
