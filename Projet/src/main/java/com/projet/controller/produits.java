package com.projet.controller;

import com.projet.entities.Produit;
import com.projet.services.ProduitService;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class produits {

    @FXML
    private FlowPane productGrid;

    private final ProduitService ps = new ProduitService();

    @FXML
    public void initialize() {
        loadProducts();
        drawer.setTranslateX(-200);
        drawer.setMouseTransparent(true);
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




    // Placeholder buttons
    @FXML
    void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    void filter() {
        System.out.println("Filtres à venir");
    }

    @FXML
    void controlproduit() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/produitcontrol.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML private VBox drawer;
    @FXML private HBox topbar;
    @FXML private AnchorPane contentPane;

    private boolean drawerOpen = false;


    @FXML
    void toggleDrawer() {

        drawer.toFront(); // always above

        TranslateTransition tt =
                new TranslateTransition(Duration.millis(250), drawer);

        if (drawerOpen) {
            tt.setToX(-200);
            drawer.setMouseTransparent(true);
        } else {
            tt.setToX(0);
            drawer.setMouseTransparent(false);
        }

        drawerOpen = !drawerOpen;
        tt.play();
    }

}
