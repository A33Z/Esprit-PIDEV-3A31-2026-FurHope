package com.projet.controller;

import com.projet.utils.ProductSceneNavigator;
import com.projet.entities.Produit;
import com.projet.services.ProduitService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class modifierproduit {

    @FXML private TextField titleField;
    @FXML private TextField priceField;
    @FXML private TextField tvaField;
    @FXML private TextField stockField;
    @FXML private TextArea descriptionField;
    @FXML private ImageView imagePreview;

    private int produitId;
    private String imagePath;

    private final ProduitService ps = new ProduitService();

    // Called from admin page
    public void loadProduit(int id) {

        this.produitId = id;

        try {
            Produit p = ps.findById(id);

            titleField.setText(p.getTitle());
            priceField.setText(String.valueOf(p.getPrice()));
            tvaField.setText(String.valueOf(p.getTva()));
            stockField.setText(String.valueOf(p.getStock()));
            descriptionField.setText(p.getDescription());

            imagePath = p.getImage();

            if (imagePath != null && !imagePath.isEmpty()) {
                imagePreview.setImage(new Image("file:" + imagePath));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Upload image
    @FXML
    void uploadImage() {

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fc.showOpenDialog(null);

        if (file != null) {
            imagePath = file.getAbsolutePath();
            imagePreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    void modifierproduit() {

        try {

            Produit p = new Produit();

            p.setId(produitId); // locked id from selected product
            p.setTitle(titleField.getText());
            p.setPrice(Double.parseDouble(priceField.getText()));
            p.setTva(Double.parseDouble(tvaField.getText()));
            p.setImage(imagePath);
            p.setDescription(descriptionField.getText());
            p.setStock(Integer.parseInt(stockField.getText()));

            ps.modifier(p);

            System.out.println("Produit mis Ã  jour !");
            goBack();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Return to admin
    @FXML
    void goBack() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/produitcontrol.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) titleField.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
