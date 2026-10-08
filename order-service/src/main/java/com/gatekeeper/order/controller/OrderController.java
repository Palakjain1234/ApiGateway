package com.gatekeeper.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo endpoints to test Gateway routing, idempotency, and method restriction.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders() {
        return ResponseEntity.ok(Map.of(
            "orders", List.of(
                Map.of("id", "ORD-001", "status", "DELIVERED", "total", 59.99),
                Map.of("id", "ORD-002", "status", "PENDING",   "total", 29.99)
            )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "PROCESSING", "total", 99.99));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return ResponseEntity.status(201).body(Map.of(
            "id",     orderId,
            "status", "PENDING",
            "body",   body
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-service"));
    }
}
