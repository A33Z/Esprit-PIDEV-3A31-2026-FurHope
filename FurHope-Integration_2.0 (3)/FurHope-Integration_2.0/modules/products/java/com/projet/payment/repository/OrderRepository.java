package com.projet.payment.repository;

import com.projet.payment.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerEmail(String customerEmail);

    Optional<Order> findByTransactionId(String transactionId);
}
