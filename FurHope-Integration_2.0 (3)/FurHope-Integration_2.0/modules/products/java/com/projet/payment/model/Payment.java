package com.projet.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type")
public abstract class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, unique = true, updatable = false)
    private String transactionId;

    @Column(nullable = false)
    private String status;

    @Column(length = 32)
    private String providerName;

    @Column(length = 255)
    private String providerPaymentId;
    
    @Column
    private String confirmationCode;
    
    @Column
    private Instant confirmationExpiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    protected Payment(String customerName, String customerEmail, BigDecimal amount, String transactionId, String status) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.transactionId = transactionId;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public Instant getConfirmationExpiresAt() {
        return confirmationExpiresAt;
    }

    public void setConfirmationExpiresAt(Instant confirmationExpiresAt) {
        this.confirmationExpiresAt = confirmationExpiresAt;
    }
}
