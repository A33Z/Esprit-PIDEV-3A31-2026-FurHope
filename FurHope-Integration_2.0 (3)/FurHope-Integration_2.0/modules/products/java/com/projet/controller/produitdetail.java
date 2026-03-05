package com.projet.controller;

import com.projet.utils.ProductSceneNavigator;
import com.projet.entities.Produit;
import com.projet.entities.Panier;
import com.projet.services.PanierService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import java.nio.file.Files;
import java.nio.file.Path;


public class produitdetail {

    @FXML private Button addToCartBtn;

    @FXML private ImageView productImage;
    @FXML private Label productName;
    @FXML private Label productPrice;
    @FXML private TextArea productDescription;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Produit currentProduit;

    public void setProduit(Produit produit) {
        this.currentProduit = produit;
        productName.setText(produit.getTitle());
        productPrice.setText(String.valueOf(produit.getPrice()));
        productDescription.setText(produit.getDescription());

        productImage.setImage(resolveImage(produit.getImage()));

        quantitySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, produit.getStock(), 1)
        );
    }



    @FXML
    void addToCart() {

        try {
            Panier panier = new Panier();
            panier.setIdProduit(currentProduit.getId());
            panier.setQty(quantitySpinner.getValue());

            PanierService ps = new PanierService();
            ps.ajouter(panier);

            goBackToShop();   // ðŸ”¥ directly return to shop

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void goBackToShop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
            Parent root = loader.load();

            produits controller = loader.getController();
            controller.loadProducts();   // ðŸ”¥ reload updated stock

            Stage stage = (Stage) addToCartBtn.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void backToShop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
            Parent root = loader.load();

            produits controller = loader.getController();
            controller.loadProducts();   // ðŸ”¥ refresh stock

            Stage stage = (Stage) addToCartBtn.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

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
