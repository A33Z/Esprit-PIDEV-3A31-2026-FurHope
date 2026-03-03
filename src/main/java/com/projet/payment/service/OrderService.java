package com.projet.payment.service;

import com.projet.payment.dto.OrderResponse;
import com.projet.payment.entity.Order;
import com.projet.payment.model.Payment;
import com.projet.payment.repository.OrderRepository;
import com.projet.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final String SUCCESS_STATUS = "SUCCESS";
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public OrderResponse createOrder(String transactionId) {
        String normalizedTransactionId = transactionId == null ? "" : transactionId.trim();
        if (normalizedTransactionId.isEmpty()) {
            return new OrderResponse(false, null, "Transaction ID is required");
        }

        Optional<Payment> optionalPayment = paymentRepository.findByTransactionId(normalizedTransactionId);
        if (optionalPayment.isEmpty()) {
            return new OrderResponse(false, null, "Payment not found");
        }

        Payment payment = optionalPayment.get();
        if (payment.getStatus() == null || !SUCCESS_STATUS.equalsIgnoreCase(payment.getStatus())) {
            return new OrderResponse(false, null, "Payment is not successful");
        }

        Optional<Order> existingOrder = orderRepository.findByTransactionId(normalizedTransactionId);
        if (existingOrder.isPresent()) {
            return new OrderResponse(false, existingOrder.get().getId(), "Order already exists for this transaction");
        }

        double totalAmount = payment.getAmount() == null ? 0.0 : payment.getAmount().doubleValue();
        Order order = new Order(
                payment.getCustomerEmail(),
                totalAmount,
                normalizedTransactionId,
                LocalDateTime.now()
        );

        try {
            Order savedOrder = orderRepository.save(order);
            return new OrderResponse(true, savedOrder.getId(), "Order created successfully");
        } catch (DataIntegrityViolationException ex) {
            Optional<Order> concurrentOrder = orderRepository.findByTransactionId(normalizedTransactionId);
            if (concurrentOrder.isPresent()) {
                return new OrderResponse(false, concurrentOrder.get().getId(), "Order already exists for this transaction");
            }
            return new OrderResponse(false, null, "Failed to create order");
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(String email) {
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isEmpty()) {
            return Collections.emptyList();
        }
        return orderRepository.findByCustomerEmail(normalizedEmail);
    }
}
