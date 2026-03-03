package com.projet.controller;

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

        String path = produit.getImage();

        if (path != null && !path.isEmpty()) {
            Image img = new Image("file:/" + path.replace("\\", "/"));
            productImage.setImage(img);
        }

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

            goBackToShop();   // 🔥 directly return to shop

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void goBackToShop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
            Parent root = loader.load();

            produits controller = loader.getController();
            controller.loadProducts();   // 🔥 reload updated stock

            Stage stage = (Stage) addToCartBtn.getScene().getWindow();
            stage.setScene(new Scene(root));

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
            controller.loadProducts();   // 🔥 refresh stock

            Stage stage = (Stage) addToCartBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 650));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
