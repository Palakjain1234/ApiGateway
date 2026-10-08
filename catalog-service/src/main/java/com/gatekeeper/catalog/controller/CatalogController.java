package com.gatekeeper.catalog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demo endpoints to test Gateway routing, header propagation, and timeout enforcement.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllItems() {
        return ResponseEntity.ok(Map.of(
            "items", List.of(
                Map.of("id", 1, "name", "Widget A", "price", 19.99),
                Map.of("id", 2, "name", "Widget B", "price", 29.99)
            )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getItem(@PathVariable int id) {
        return ResponseEntity.ok(Map.of("id", id, "name", "Widget " + id, "price", 19.99 * id));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "catalog-service"));
    }
}
