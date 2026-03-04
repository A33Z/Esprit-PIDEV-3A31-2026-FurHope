package com.projet.controller;

import com.projet.entities.Panier;
import com.projet.payment.client.OrderApiClient;
import com.projet.payment.dto.OrderResponse;
import com.projet.payment.PromoApiEndpoints;
import com.projet.payment.SpringPaymentServer;
import com.projet.services.PanierService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cartcontrol {

    @FXML private TableView<Panier> cartTable;
    @FXML private TableColumn<Panier, String> colName;
    @FXML private TableColumn<Panier, Integer> colQty;
    @FXML private TableColumn<Panier, Double> colTotal;
    @FXML private TableColumn<Panier, Void> colDelete;
    @FXML private Label totalLabel;
    @FXML private TextField customerNameField;
    @FXML private TextField customerEmailField;
    @FXML private ChoiceBox<String> paymentMethodChoice;
    @FXML private TextField cardNumberField;
    @FXML private TextField paypalEmailField;

    private final PanierService ps = new PanierService();
    private final OrderApiClient orderApiClient = new OrderApiClient();
    private static final Pattern STATUS_PATTERN = Pattern.compile("\\\"status\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile("\\\"transactionId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern PROMO_VALID_PATTERN = Pattern.compile("\\\"valid\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROMO_DISCOUNT_PATTERN = Pattern.compile("\\\"discount\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern PROMO_FINAL_AMOUNT_PATTERN = Pattern.compile("\\\"finalAmount\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @FXML
    public void initialize() {
        try {

            colName.setCellValueFactory(new PropertyValueFactory<>("title"));
            colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
            colTotal.setCellValueFactory(new PropertyValueFactory<>("totalP"));
            cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            colDelete.setCellFactory(param -> new TableCell<>() {
                private final Spinner<Integer> qtySpinner = new Spinner<>();
                private final Button btn = new Button("Remove");
                private final HBox container = new HBox(8, qtySpinner, btn);

                {
                    qtySpinner.setEditable(true);
                    qtySpinner.setPrefWidth(80);

                    btn.setOnAction(e -> {
                        Panier p = getTableView().getItems().get(getIndex());
                        try {
                            int qtyToRemove = qtySpinner.getValue();
                            ps.supprimerQuantite(p.getId(), qtyToRemove);
                            refresh();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        return;
                    }

                    Panier p = getTableView().getItems().get(getIndex());
                    int maxQty = Math.max(1, p.getQty());
                    qtySpinner.setValueFactory(
                            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxQty, 1)
                    );
                    setGraphic(container);
                }
            });

            paymentMethodChoice.getItems().setAll("CARD", "PAYPAL");
            paymentMethodChoice.setValue("CARD");
            updatePaymentFieldsVisibility();
            paymentMethodChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePaymentFieldsVisibility());

            refresh();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refresh() throws SQLException {
        List<Panier> paniers = ps.afficher();
        cartTable.getItems().setAll(paniers);

        double total = paniers.stream()
                .mapToDouble(Panier::getTotalP)
                .sum();

        totalLabel.setText("Total: " + total);
    }

    @FXML
    void proceedPayment() {
        try {
            List<Panier> paniers = ps.afficher();
            if (paniers.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Checkout", "Your cart is empty.");
                return;
            }

            String customerName = customerNameField.getText() == null ? "" : customerNameField.getText().trim();
            if (customerName.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Checkout", "Customer name is required.");
                return;
            }
            
            String customerEmail = customerEmailField.getText() == null ? "" : customerEmailField.getText().trim();
            if (!EMAIL_PATTERN.matcher(customerEmail).matches()) {
                showAlert(Alert.AlertType.WARNING, "Checkout", "Enter a valid customer email.");
                return;
            }

            String paymentMethod = paymentMethodChoice.getValue();
            if (paymentMethod == null || paymentMethod.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Checkout", "Select a payment method.");
                return;
            }

            if (!validatePaymentDetails(paymentMethod)) {
                return;
            }

            BigDecimal totalAmount = calculateTotal(paniers);
            BigDecimal amountToPay = totalAmount;

            Optional<String> promoCodeInput = askForPromoCode();
            if (promoCodeInput.isPresent()) {
                String promoCode = promoCodeInput.get().trim();
                if (!promoCode.isEmpty()) {
                    PromoResult promoResult = applyPromoCode(promoCode, totalAmount);
                    if (promoResult.valid) {
                        amountToPay = promoResult.finalAmount;
                    }
                    showAlert(
                            promoResult.valid ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                            "Promo Code",
                            promoResult.message + "\nDiscount: " + promoResult.discount + "\nAmount to pay: " + promoResult.finalAmount
                    );
                }
            }

            if (!confirmPayment(customerName, paymentMethod, amountToPay)) {
                return;
            }
            String requestBody = buildPaymentRequestJson(customerName, customerEmail, amountToPay, paymentMethod);
            String responseBody = callPaymentApi(requestBody);

            String status = extractValue(STATUS_PATTERN, responseBody);
            String transactionId = extractValue(TRANSACTION_PATTERN, responseBody);
            String message = extractValue(MESSAGE_PATTERN, responseBody);

            if ("PENDING".equalsIgnoreCase(status)) {
                showAlert(Alert.AlertType.INFORMATION, "Email Verification", message + "\nCheck your email and enter the code.");
                Optional<String> confirmationCode = askForConfirmationCode();
                if (confirmationCode.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Checkout", "Payment still pending. Confirmation code required.");
                    return;
                }
                String confirmResponse = callPaymentConfirmApi(buildConfirmRequestJson(transactionId, confirmationCode.get().trim()));
                String finalStatus = extractValue(STATUS_PATTERN, confirmResponse);
                String finalTransactionId = extractValue(TRANSACTION_PATTERN, confirmResponse);
                String finalMessage = extractValue(MESSAGE_PATTERN, confirmResponse);
                showPaymentResult(finalStatus, finalTransactionId, finalMessage);
                if ("SUCCESS".equalsIgnoreCase(finalStatus)) {
                    handleOrderAfterSuccessfulPayment(finalTransactionId);
                    finalizeSuccessfulCheckout();
                }
                return;
            }

            showPaymentResult(status, transactionId, message);
            if ("SUCCESS".equalsIgnoreCase(status)) {
                handleOrderAfterSuccessfulPayment(transactionId);
                finalizeSuccessfulCheckout();
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Checkout Error", "Failed to read cart data: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            showAlert(Alert.AlertType.ERROR, "Checkout Error", "Payment request interrupted.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Checkout Error", "Payment/Promo/Order API unavailable: " + e.getMessage());
        }
    }

    private BigDecimal calculateTotal(List<Panier> paniers) {
        double total = paniers.stream().mapToDouble(Panier::getTotalP).sum();
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildPaymentRequestJson(String customerName, String customerEmail, BigDecimal amount, String paymentMethod) {
        return "{"
                + "\"customerName\":\"" + escapeJson(customerName) + "\"," 
                + "\"customerEmail\":\"" + escapeJson(customerEmail) + "\","
                + "\"amount\":" + amount.toPlainString() + ","
                + "\"paymentMethod\":\"" + paymentMethod + "\""
                + "}";
    }

    private String callPaymentApi(String requestBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SpringPaymentServer.getPaymentProcessUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Payment API returned HTTP " + response.statusCode());
        }

        return response.body();
    }

    private void handleOrderAfterSuccessfulPayment(String transactionId) throws IOException, InterruptedException {
        OrderResponse orderResponse = orderApiClient.createOrder(transactionId);
        if (orderResponse.isSuccess()) {
            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Order Created",
                    "Order ID: " + orderResponse.getOrderId() + "\n" + orderResponse.getMessage()
            );
        } else {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Order Creation",
                    orderResponse.getMessage()
            );
        }
    }

    private PromoResult applyPromoCode(String code, BigDecimal amount) throws IOException, InterruptedException {
        String requestBody = "{"
                + "\"code\":\"" + escapeJson(code) + "\","
                + "\"amount\":" + amount.toPlainString()
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PromoApiEndpoints.getPromoApplyUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Promo API returned HTTP " + response.statusCode());
        }

        String payload = response.body();
        boolean valid = extractBooleanValue(PROMO_VALID_PATTERN, payload);
        BigDecimal discount = extractDecimalValue(PROMO_DISCOUNT_PATTERN, payload);
        BigDecimal finalAmount = extractDecimalValue(PROMO_FINAL_AMOUNT_PATTERN, payload);
        String message = extractValue(MESSAGE_PATTERN, payload);

        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            finalAmount = amount;
        }

        return new PromoResult(valid, discount, finalAmount, message);
    }
    
    private String callPaymentConfirmApi(String requestBody) throws IOException, InterruptedException {
        String processUrl = SpringPaymentServer.getPaymentProcessUrl();
        String confirmUrl = processUrl.replace("/process", "/confirm");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(confirmUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Payment confirm API returned HTTP " + response.statusCode());
        }

        return response.body();
    }

    private String buildConfirmRequestJson(String transactionId, String confirmationCode) {
        return "{"
                + "\"transactionId\":\"" + escapeJson(transactionId) + "\","
                + "\"confirmationCode\":\"" + escapeJson(confirmationCode) + "\""
                + "}";
    }

    private String extractValue(Pattern pattern, String payload) {
        Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "N/A";
    }

    private void updatePaymentFieldsVisibility() {
        String paymentMethod = paymentMethodChoice.getValue();
        boolean isCard = "CARD".equals(paymentMethod);
        cardNumberField.setDisable(!isCard);
        paypalEmailField.setDisable(isCard);
    }

    private boolean validatePaymentDetails(String paymentMethod) {
        if ("CARD".equals(paymentMethod)) {
            String digitsOnly = cardNumberField.getText() == null ? "" : cardNumberField.getText().replaceAll("\\s+", "");
            if (!digitsOnly.matches("\\d{16}") || !isValidLuhn(digitsOnly)) {
                showAlert(Alert.AlertType.WARNING, "Checkout", "Enter a valid 16-digit card number.");
                return false;
            }
            return true;
        }

        String paypalEmail = paypalEmailField.getText() == null ? "" : paypalEmailField.getText().trim();
        if (!EMAIL_PATTERN.matcher(paypalEmail).matches()) {
            showAlert(Alert.AlertType.WARNING, "Checkout", "Enter a valid PayPal email.");
            return false;
        }
        return true;
    }

    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private boolean confirmPayment(String customerName, String paymentMethod, BigDecimal totalAmount) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Payment");
        confirm.setHeaderText("Please confirm checkout");
        confirm.setContentText(
                "Customer: " + customerName
                        + "\nMethod: " + paymentMethod
                        + "\nAmount: " + totalAmount.toPlainString()
        );
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showPaymentResult(String status, String transactionId, String message) {
        if ("SUCCESS".equalsIgnoreCase(status)) {
            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Payment Success",
                    "Status: " + status + "\nTransaction ID: " + transactionId + "\n" + message
            );
            return;
        }

        showAlert(
                Alert.AlertType.ERROR,
                "Payment Failed",
                "Status: " + status + "\nTransaction ID: " + transactionId + "\n" + message
        );
    }

    private Optional<String> askForConfirmationCode() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Email Confirmation");
        dialog.setHeaderText("Enter confirmation code");
        dialog.setContentText("Code:");
        return dialog.showAndWait();
    }

    private Optional<String> askForPromoCode() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Promo Code");
        dialog.setHeaderText("Apply a promo code (optional)");
        dialog.setContentText("Promo code:");
        return dialog.showAndWait();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean extractBooleanValue(Pattern pattern, String payload) {
        Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return false;
    }

    private BigDecimal extractDecimalValue(Pattern pattern, String payload) {
        Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void finalizeSuccessfulCheckout() throws SQLException {
        ps.validerPaiementEtViderPanier();
        refresh();
        clearCheckoutForm();
    }

    private void clearCheckoutForm() {
        customerNameField.clear();
        customerEmailField.clear();
        cardNumberField.clear();
        paypalEmailField.clear();
        paymentMethodChoice.setValue("CARD");
        updatePaymentFieldsVisibility();
    }

    private static final class PromoResult {
        private final boolean valid;
        private final BigDecimal discount;
        private final BigDecimal finalAmount;
        private final String message;

        private PromoResult(boolean valid, BigDecimal discount, BigDecimal finalAmount, String message) {
            this.valid = valid;
            this.discount = discount;
            this.finalAmount = finalAmount;
            this.message = message == null || message.isBlank() ? "Promo result received." : message;
        }
    }

    @FXML
    void backToShop() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shop.fxml"));
            Parent root = loader.load();

            produits controller = loader.getController();
            controller.loadProducts();

            Stage stage = (Stage) cartTable.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
