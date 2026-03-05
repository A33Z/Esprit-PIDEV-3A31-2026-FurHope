package com.projet.payment.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("STRIPE")
public class StripePayment extends Payment {

    protected StripePayment() {
    }

    public StripePayment(String customerName, String customerEmail, BigDecimal amount, String transactionId, String status) {
        super(customerName, customerEmail, amount, transactionId, status);
    }
}
