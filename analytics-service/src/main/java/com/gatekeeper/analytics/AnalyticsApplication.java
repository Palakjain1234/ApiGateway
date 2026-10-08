package com.gatekeeper.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GateKeeper — Analytics Service
 *
 * Consumes Gateway access events from Kafka, stores them in MongoDB,
 * and exposes operational statistics endpoints.
 *
 * Port: 8082
 */
@SpringBootApplication
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
