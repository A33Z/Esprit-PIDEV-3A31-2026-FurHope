package com.projet.controller;

import com.projet.utils.ProductSceneNavigator;
import com.projet.entities.Produit;
import com.projet.services.ProduitService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class produitcontrol {

    @FXML
    private ListView<Produit> listView;

    private final ProduitService ps = new ProduitService();

    @FXML
    public void initialize() {
        refresh();
        listView.setCellFactory(param -> new ProduitCell());
    }

    void refresh() {
        try {
            listView.setItems(FXCollections.observableArrayList(ps.afficher()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Open edit screen
    void openEditPage(int id) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierproduit.fxml"));
            Parent root = loader.load();

            modifierproduit controller = loader.getController();
            controller.loadProduit(id);

            Stage stage = (Stage) listView.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Custom ListView cell
    class ProduitCell extends ListCell<Produit> {

        @Override
        protected void updateItem(Produit p, boolean empty) {
            super.updateItem(p, empty);

            if (empty || p == null) {
                setGraphic(null);
                return;
            }

            ImageView img = new ImageView();
            img.setFitWidth(100);
            img.setFitHeight(80);
            img.setPreserveRatio(true);

            if (p.getImage() != null && !p.getImage().isEmpty()) {
                img.setImage(new Image("file:" + p.getImage()));
            }

            Label title = new Label(p.getTitle());
            title.getStyleClass().add("product-name");

            Label price = new Label("Prix: " + p.getPrice());
            Label tva = new Label("TVA: " + p.getTva());
            Label stock = new Label("Stock: " + p.getStock());

            HBox metrics = new HBox(15, price, tva, stock);
            metrics.setAlignment(Pos.CENTER_LEFT);

            Label desc = new Label(p.getDescription());
            desc.setWrapText(true);
            desc.setMaxWidth(450);
            desc.getStyleClass().add("product-desc");

            VBox info = new VBox(6, title, metrics, desc);
            info.setAlignment(Pos.CENTER_LEFT);

            Button delete = new Button("Supprimer");
            Button update = new Button("Modifier");

            delete.getStyleClass().add("danger-button");
            update.getStyleClass().add("primary-button");

            delete.setOnAction(e -> {
                try {
                    ps.supprimer(p.getId());
                    refresh();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            update.setOnAction(e -> openEditPage(p.getId()));

            VBox buttons = new VBox(10, update, delete);
            buttons.setAlignment(Pos.CENTER);

            HBox root = new HBox(25, img, info, buttons);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("product-card");

            setGraphic(root);
        }
    }





    @FXML
    void addproduit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterProduit.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) listView.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openDetail(Produit produit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/productdetail.fxml"));
            Parent root = loader.load();

            produitdetail controller = loader.getController();
            controller.setProduit(produit);

            Stage stage = new Stage();
            ProductSceneNavigator.setScene(stage, root);
            stage.setTitle("Product Detail");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goBack() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/shop.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) listView.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
