package com.projet.payment.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("PAYPAL")
public class PaypalPayment extends Payment {

    protected PaypalPayment() {
    }

    public PaypalPayment(String customerName, String customerEmail, BigDecimal amount, String transactionId, String status) {
        super(customerName, customerEmail, amount, transactionId, status);
    }
}
