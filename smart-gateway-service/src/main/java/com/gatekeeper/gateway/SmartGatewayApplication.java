package com.gatekeeper.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * GateKeeper — Smart Gateway Service
 *
 * Data plane: matches incoming requests to routes, enforces Gateway
 * policies (rate limiting, idempotency, header manipulation, timeouts)
 * and forwards traffic to target backends via WebClient.
 *
 * Port: 8081
 */
@SpringBootApplication
@EnableScheduling
public class SmartGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartGatewayApplication.class, args);
    }
}
