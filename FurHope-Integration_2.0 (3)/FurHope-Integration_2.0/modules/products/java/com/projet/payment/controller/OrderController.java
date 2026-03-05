package com.projet.payment.controller;

import com.projet.payment.dto.OrderCreateRequest;
import com.projet.payment.dto.OrderHistoryResponse;
import com.projet.payment.dto.OrderResponse;
import com.projet.payment.entity.Order;
import com.projet.payment.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(request.getTransactionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{email:.+}")
    public ResponseEntity<List<OrderHistoryResponse>> getOrdersByCustomer(@PathVariable("email") String email) {
        List<OrderHistoryResponse> response = orderService.getOrdersByCustomer(email)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private OrderHistoryResponse toHistoryResponse(Order order) {
        return new OrderHistoryResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getTransactionId(),
                order.getCreatedAt()
        );
    }
}
