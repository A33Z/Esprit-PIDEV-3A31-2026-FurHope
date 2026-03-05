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

public class ajouterproduit {

    @FXML private TextField titleField;
    @FXML private TextField priceField;
    @FXML private TextField tvaField;
    @FXML private TextField stockField;
    @FXML private TextArea descriptionField;
    @FXML private ImageView imagePreview;

    private String imagePath;

    ProduitService ps = new ProduitService();

    // Upload image
    @FXML

    void uploadImage()  {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            imagePath = file.getAbsolutePath();
            Image image = new Image(file.toURI().toString());
            imagePreview.setImage(image);
        }
    }

    // Add product
    @FXML
    void ajouterProduit() {

        try {
            Produit p = new Produit();

            p.setTitle(titleField.getText());
            p.setPrice(Double.parseDouble(priceField.getText()));
            p.setTva(Double.parseDouble(tvaField.getText()));
            p.setImage(imagePath);
            p.setDescription(descriptionField.getText());
            p.setStock(Integer.parseInt(stockField.getText()));

            ps.ajouter(p);

            System.out.println("Produit ajoutÃ© !");
            clearForm();

        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }


    // Reset form
    void clearForm() {
        titleField.clear();
        priceField.clear();
        tvaField.clear();
        stockField.clear();
        descriptionField.clear();
        imagePreview.setImage(null);
        imagePath = null;
    }
    @FXML
    void controlproduit() {

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
