package com.projet.controller;

import com.projet.utils.ProductSceneNavigator;
import com.projet.entities.Panier;
import com.projet.entities.Produit;
import com.projet.services.PanierService;
import com.projet.services.ProduitService;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Button;



public class produits {

    @FXML
    private FlowPane productGrid;

    private final ProduitService ps = new ProduitService();

    @FXML
    public void initialize() {
        try {
            ps.ensureSeedData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadProducts();
        updateCartButton();
        drawer.setTranslateX(-200);
        drawer.setMouseTransparent(true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });
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
                img.setImage(resolveImage(p.getImage()));
            } catch (Exception e) {
                img.setImage(resolveImage("placeholder-product.png"));
            }
        } else {
            img.setImage(resolveImage("placeholder-product.png"));
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

//  CLICK â†’ OPEN DETAIL
        card.setOnMouseClicked(e -> openDetail(p));

        return card;
    }

    private void openDetail(Produit produit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/productdetail.fxml"));
            Parent root = loader.load();

            produitdetail controller = loader.getController();
            controller.setProduit(produit);

            Stage stage = (Stage) productGrid.getScene().getWindow(); // ðŸ”¥ same stage
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Placeholder buttons
    @FXML
    void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    void filter() {
        System.out.println("Filtres Ã  venir");
    }

    @FXML
    void controlproduit() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/produitcontrol.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openOrderHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/orderhistory.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private Button cartBtn;
    @FXML private VBox drawer;
    @FXML private HBox topbar;
    @FXML private AnchorPane contentPane;
    @FXML private TextField searchField;
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

    @FXML
    void openCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cart.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCartButton() {
        try {
            int count = new PanierService().afficher()
                    .stream()
                    .mapToInt(Panier::getQty)
                    .sum();
            cartBtn.setText("Cart (" + count + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterProducts(String keyword) {
        try {
            productGrid.getChildren().clear();

            for (Produit p : ps.afficher()) {

                if (p.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                    productGrid.getChildren().add(createCard(p));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Image resolveImage(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            var fallback = getClass().getResource("/placeholder-product.png");
            return fallback == null ? null : new Image(fallback.toExternalForm());
        }

        try {
            Path path = Path.of(rawPath);
            if (Files.exists(path)) {
                return new Image(path.toUri().toString());
            }
        } catch (Exception ignored) {
            // Continue to classpath fallback.
        }

        String normalized = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
        var resource = getClass().getResource(normalized);
        if (resource != null) {
            return new Image(resource.toExternalForm());
        }

        var fallback = getClass().getResource("/placeholder-product.png");
        return fallback == null ? null : new Image(fallback.toExternalForm());
    }
}
