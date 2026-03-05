package com.projet.controller;

import com.projet.utils.ProductSceneNavigator;
import com.projet.payment.SpringPaymentServer;
import com.projet.payment.client.OrderApiClient;
import com.projet.payment.dto.OrderHistoryResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class orderhistory {

    @FXML
    private TextField emailField;

    @FXML
    private TableView<OrderHistoryResponse> ordersTable;

    @FXML
    private TableColumn<OrderHistoryResponse, Long> colOrderId;

    @FXML
    private TableColumn<OrderHistoryResponse, Double> colAmount;

    @FXML
    private TableColumn<OrderHistoryResponse, String> colTransaction;

    @FXML
    private TableColumn<OrderHistoryResponse, LocalDateTime> colCreatedAt;

    private final OrderApiClient orderApiClient = new OrderApiClient();

    @FXML
    public void initialize() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colTransaction.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    void loadOrderHistory() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Order History", "Please enter a customer email.");
            return;
        }

        try {
            SpringPaymentServer.start();
            List<OrderHistoryResponse> orders = orderApiClient.getOrdersByCustomer(email);
            ordersTable.getItems().setAll(orders);

            if (orders.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Order History", "No orders found for this customer.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showAlert(Alert.AlertType.ERROR, "Order History", "Request interrupted.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Order History", "Order API unavailable: " + e.getMessage());
        }
    }

    @FXML
    void backToShop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
            Parent root = loader.load();

            produits controller = loader.getController();
            controller.loadProducts();

            Stage stage = (Stage) ordersTable.getScene().getWindow();
            ProductSceneNavigator.setScene(stage, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
