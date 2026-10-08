package com.gatekeeper.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * GateKeeper — API Management Service
 *
 * Control plane: manages organizations, management users, API routes,
 * configuration versioning and audit records.
 *
 * Port: 8080
 */
@SpringBootApplication
@EnableAsync
public class ApiManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiManagementApplication.class, args);
    }
}
