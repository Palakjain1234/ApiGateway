package com.gatekeeper.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GateKeeper — Catalog Service (Demo)
 * Simulates a tenant's product catalog backend. Port: 8091
 */
@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
