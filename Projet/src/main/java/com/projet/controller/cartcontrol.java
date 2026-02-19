package com.projet.controller;

import com.projet.entities.Panier;
import com.projet.services.PanierService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class cartcontrol {

    @FXML private TableView<Panier> cartTable;
    @FXML private TableColumn<Panier, String> colName;
    @FXML private TableColumn<Panier, Integer> colQty;
    @FXML private TableColumn<Panier, Double> colTotal;
    @FXML private TableColumn<Panier, Void> colDelete;
    @FXML private Label totalLabel;

    private final PanierService ps = new PanierService();

    @FXML
    public void initialize() {
        try {

            colName.setCellValueFactory(new PropertyValueFactory<>("title"));
            colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
            colTotal.setCellValueFactory(new PropertyValueFactory<>("totalP"));
            cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


            // delete button column
            colDelete.setCellFactory(param -> new TableCell<>() {
                private final Button btn = new Button("Delete");

                {
                    btn.setOnAction(e -> {
                        Panier p = getTableView().getItems().get(getIndex());
                        try {
                            ps.supprimer(p.getId());
                            refresh();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });

            refresh();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refresh() throws Exception {

        cartTable.getItems().setAll(ps.afficher());

        double total = ps.afficher()
                .stream()
                .mapToDouble(Panier::getTotalP)
                .sum();

        totalLabel.setText("Total: " + total);
    }

    @FXML
    void proceedPayment() {
        System.out.println("Payment coming soon 😄");
    }

    @FXML
    void backToShop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
            Parent root = loader.load();

            produits controller = loader.getController();
            controller.loadProducts();   // 🔥 refresh stock

            Stage stage = (Stage) cartTable.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
