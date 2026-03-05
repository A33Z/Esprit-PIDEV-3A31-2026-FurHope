package com.projet.controller;

import com.projet.utils.ProductSceneNavigator;
import com.projet.entities.Panier;
import com.projet.payment.client.OrderApiClient;
import com.projet.payment.dto.OrderResponse;
import com.projet.payment.PromoApiEndpoints;
import com.projet.payment.SpringPaymentServer;
import com.projet.services.CurrencyRateService;
import com.projet.services.PanierService;
import java.awt.Desktop;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cartcontrol {

    @FXML private TableView<Panier> cartTable;
    @FXML private TableColumn<Panier, String> colName;
    @FXML private TableColumn<Panier, Integer> colQty;
    @FXML private TableColumn<Panier, Double> colTotal;
    @FXML private TableColumn<Panier, Void> colDelete;
    @FXML private Label totalLabel;
    @FXML private Label exchangeRateLabel;
    @FXML private TextField customerNameField;
    @FXML private TextField customerEmailField;
    @FXML private ChoiceBox<String> paymentMethodChoice;
    @FXML private ChoiceBox<String> currencyChoice;
    @FXML private TextField cardNumberField;
    @FXML private TextField paypalEmailField;

    private final PanierService ps = new PanierService();
    private final OrderApiClient orderApiClient = new OrderApiClient();
    private final CurrencyRateService currencyRateService = new CurrencyRateService();
    private static final String BASE_CURRENCY = "USD";
    private static final Pattern STATUS_PATTERN = Pattern.compile("\\\"status\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile("\\\"transactionId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern CHECKOUT_URL_PATTERN = Pattern.compile("\\\"checkoutUrl\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern PROMO_VALID_PATTERN = Pattern.compile("\\\"valid\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROMO_DISCOUNT_PATTERN = Pattern.compile("\\\"discount\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern PROMO_FINAL_AMOUNT_PATTERN = Pattern.compile("\\\"finalAmount\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private BigDecimal baseTotalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal currentExchangeRate = BigDecimal.ONE;
    private String selectedCurrency = BASE_CURRENCY;

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

            paymentMethodChoice.getItems().setAll("CARD", "PAYPAL", "STRIPE");
            paymentMethodChoice.setValue("CARD");
            updatePaymentFieldsVisibility();
            paymentMethodChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePaymentFieldsVisibility());
            setupCurrencyChoice();

            refresh();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refresh() throws SQLException {
        List<Panier> paniers = ps.afficher();
        cartTable.getItems().setAll(paniers);

        baseTotalAmount = calculateTotal(paniers);
        updateDisplayedTotalLabel();
    }

    @FXML
    void proceedPayment() {
        try {
            SpringPaymentServer.start();
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
                            promoResult.message
                                    + "\nDiscount: " + promoResult.discount + " " + BASE_CURRENCY
                                    + "\nAmount to pay: " + formatAmount(convertAmount(promoResult.finalAmount), selectedCurrency)
                                    + " (base " + formatAmount(promoResult.finalAmount, BASE_CURRENCY) + ")"
                    );
                }
            }

            BigDecimal convertedAmountToPay = convertAmount(amountToPay);

            if (!confirmPayment(customerName, paymentMethod, convertedAmountToPay, selectedCurrency)) {
                return;
            }
            String requestBody = buildPaymentRequestJson(customerName, customerEmail, convertedAmountToPay, paymentMethod);
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
                String finalCheckoutUrl = extractValue(CHECKOUT_URL_PATTERN, confirmResponse);

                if ("STRIPE".equalsIgnoreCase(paymentMethod)) {
                    String txForStripe = "N/A".equalsIgnoreCase(finalTransactionId) ? transactionId : finalTransactionId;
                    handleStripeCheckout(finalStatus, txForStripe, finalMessage, finalCheckoutUrl);
                    return;
                }

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
        boolean isCard = "CARD".equalsIgnoreCase(paymentMethod);
        boolean isPaypal = "PAYPAL".equalsIgnoreCase(paymentMethod);
        cardNumberField.setDisable(!isCard);
        paypalEmailField.setDisable(!isPaypal);
    }

    private boolean validatePaymentDetails(String paymentMethod) {
        if ("STRIPE".equalsIgnoreCase(paymentMethod)) {
            return true;
        }

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

    private boolean confirmPayment(String customerName, String paymentMethod, BigDecimal totalAmount, String currency) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Payment");
        confirm.setHeaderText("Please confirm checkout");
        confirm.setContentText(
                "Customer: " + customerName
                        + "\nMethod: " + paymentMethod
                        + "\nAmount: " + formatAmount(totalAmount, currency)
                        + "\nRate: 1 " + BASE_CURRENCY + " = " + currentExchangeRate.toPlainString() + " " + selectedCurrency
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
        if (currencyChoice != null) {
            currencyChoice.setValue(BASE_CURRENCY);
        }
        updatePaymentFieldsVisibility();
    }

    private void setupCurrencyChoice() {
        if (currencyChoice == null) {
            return;
        }
        currencyChoice.getItems().setAll("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF");
        currencyChoice.setValue(BASE_CURRENCY);
        selectedCurrency = BASE_CURRENCY;
        currentExchangeRate = BigDecimal.ONE;
        updateExchangeRateLabel("Rate: 1 USD = 1 USD");

        currencyChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            String targetCurrency = (newVal == null || newVal.isBlank()) ? BASE_CURRENCY : newVal.trim().toUpperCase();
            selectedCurrency = targetCurrency;

            if (BASE_CURRENCY.equals(targetCurrency)) {
                currentExchangeRate = BigDecimal.ONE;
                updateExchangeRateLabel("Rate: 1 USD = 1 USD");
                updateDisplayedTotalLabel();
                return;
            }

            updateExchangeRateLabel("Loading rate...");
            CompletableFuture.supplyAsync(() -> {
                try {
                    return currencyRateService.fetchRate(BASE_CURRENCY, targetCurrency);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(rate -> Platform.runLater(() -> {
                currentExchangeRate = rate;
                updateExchangeRateLabel(
                        "Rate: 1 " + BASE_CURRENCY + " = " + rate.setScale(4, RoundingMode.HALF_UP).toPlainString() + " " + targetCurrency
                );
                updateDisplayedTotalLabel();
            })).exceptionally(ex -> {
                Platform.runLater(() -> {
                    currentExchangeRate = BigDecimal.ONE;
                    selectedCurrency = BASE_CURRENCY;
                    if (!BASE_CURRENCY.equals(currencyChoice.getValue())) {
                        currencyChoice.setValue(BASE_CURRENCY);
                    }
                    updateExchangeRateLabel("Rate unavailable. Reverted to USD.");
                    updateDisplayedTotalLabel();
                    showAlert(Alert.AlertType.WARNING, "Currency API", "Could not fetch exchange rates right now.");
                });
                return null;
            });
        });
    }

    private BigDecimal convertAmount(BigDecimal amountInBaseCurrency) {
        if (amountInBaseCurrency == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (BASE_CURRENCY.equalsIgnoreCase(selectedCurrency)) {
            return amountInBaseCurrency.setScale(2, RoundingMode.HALF_UP);
        }
        return amountInBaseCurrency
                .multiply(currentExchangeRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void updateDisplayedTotalLabel() {
        if (totalLabel == null) {
            return;
        }
        BigDecimal convertedTotal = convertAmount(baseTotalAmount);
        totalLabel.setText("Total: " + formatAmount(convertedTotal, selectedCurrency));
    }

    private String formatAmount(BigDecimal amount, String currency) {
        BigDecimal normalized = amount == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : amount.setScale(2, RoundingMode.HALF_UP);
        return normalized.toPlainString() + " " + (currency == null ? BASE_CURRENCY : currency.toUpperCase());
    }

    private void updateExchangeRateLabel(String text) {
        if (exchangeRateLabel != null) {
            exchangeRateLabel.setText(text);
        }
    }

    private void handleStripeCheckout(String status, String transactionId, String message, String checkoutUrl)
            throws IOException, InterruptedException, SQLException {
        if ("FAILED".equalsIgnoreCase(status)) {
            showPaymentResult(status, transactionId, message);
            return;
        }

        String normalizedUrl = checkoutUrl == null ? "" : checkoutUrl.replace("\\/", "/").trim();
        if (normalizedUrl.isEmpty() || "N/A".equalsIgnoreCase(normalizedUrl)) {
            showAlert(Alert.AlertType.ERROR, "Stripe Checkout", "Stripe checkout URL is missing. " + message);
            return;
        }

        openCheckoutInBrowser(normalizedUrl);

        if (!askStripeVerificationNow()) {
            showAlert(Alert.AlertType.INFORMATION, "Stripe Checkout", "Payment is pending until you verify it after checkout.");
            return;
        }

        verifyStripePayment(transactionId);
    }

    private void verifyStripePayment(String transactionId) throws IOException, InterruptedException, SQLException {
        String verifyRequest = buildStripeVerifyRequestJson(transactionId);

        for (int attempt = 1; attempt <= 5; attempt++) {
            String verifyResponse = callStripeVerifyApi(verifyRequest);
            String verifyStatus = extractValue(STATUS_PATTERN, verifyResponse);
            String verifyTransactionId = extractValue(TRANSACTION_PATTERN, verifyResponse);
            String verifyMessage = extractValue(MESSAGE_PATTERN, verifyResponse);

            String finalTransactionId = "N/A".equalsIgnoreCase(verifyTransactionId) ? transactionId : verifyTransactionId;
            if ("SUCCESS".equalsIgnoreCase(verifyStatus)) {
                showPaymentResult(verifyStatus, finalTransactionId, verifyMessage);
                handleOrderAfterSuccessfulPayment(finalTransactionId);
                finalizeSuccessfulCheckout();
                return;
            }

            if ("FAILED".equalsIgnoreCase(verifyStatus)) {
                showPaymentResult(verifyStatus, finalTransactionId, verifyMessage);
                return;
            }

            if (!askRetryStripeVerification(attempt, verifyMessage)) {
                return;
            }
        }

        showAlert(Alert.AlertType.WARNING, "Stripe Checkout", "Payment is still pending. You can verify it again later.");
    }

    private String callStripeVerifyApi(String requestBody) throws IOException, InterruptedException {
        String processUrl = SpringPaymentServer.getPaymentProcessUrl();
        String verifyUrl = processUrl.replace("/process", "/verify-stripe");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(verifyUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Stripe verify API returned HTTP " + response.statusCode());
        }

        return response.body();
    }

    private String buildStripeVerifyRequestJson(String transactionId) {
        return "{"
                + "\"transactionId\":\"" + escapeJson(transactionId) + "\""
                + "}";
    }

    private void openCheckoutInBrowser(String checkoutUrl) {
        if (tryOpenCheckoutInBrowser(checkoutUrl)) {
            return;
        }
        showCheckoutUrlDialog(checkoutUrl);
    }

    private boolean tryOpenCheckoutInBrowser(String checkoutUrl) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(checkoutUrl));
                return true;
            }
        } catch (Exception ignored) {
            // Continue with OS-level fallbacks.
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        try {
            if (osName.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", checkoutUrl).start();
                return true;
            }
            if (osName.contains("mac")) {
                new ProcessBuilder("open", checkoutUrl).start();
                return true;
            }
            if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                new ProcessBuilder("xdg-open", checkoutUrl).start();
                return true;
            }
        } catch (Exception ignored) {
            // Fallback dialog below.
        }

        return false;
    }

    private void showCheckoutUrlDialog(String checkoutUrl) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Stripe Checkout");
        dialog.setHeaderText("Browser did not open automatically");

        TextArea urlArea = new TextArea(checkoutUrl);
        urlArea.setWrapText(true);
        urlArea.setEditable(false);
        urlArea.setPrefRowCount(6);
        dialog.getDialogPane().setContent(urlArea);

        ButtonType openButton = new ButtonType("Open Browser", ButtonBar.ButtonData.OK_DONE);
        ButtonType copyButton = new ButtonType("Copy URL", ButtonBar.ButtonData.OTHER);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(openButton, copyButton, closeButton);

        while (true) {
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }

            ButtonType chosen = result.get();
            if (chosen == openButton) {
                if (tryOpenCheckoutInBrowser(checkoutUrl)) {
                    return;
                }
                continue;
            }

            if (chosen == copyButton) {
                ClipboardContent content = new ClipboardContent();
                content.putString(checkoutUrl);
                Clipboard.getSystemClipboard().setContent(content);
                continue;
            }

            return;
        }
    }

    private boolean askStripeVerificationNow() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Stripe Checkout");
        dialog.setHeaderText("Complete payment in your browser");
        dialog.setContentText("After completing the Stripe payment, click OK to verify it now.");
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private boolean askRetryStripeVerification(int attempt, String message) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Stripe Verification Pending");
        dialog.setHeaderText("Attempt " + attempt + " did not confirm payment yet");
        dialog.setContentText(message + "\n\nClick OK to verify again.");
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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
            ProductSceneNavigator.setScene(stage, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
