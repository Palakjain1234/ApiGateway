package com.gatekeeper.gateway.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Enriched request context built after route matching.
 * Passed through the policy filter chain before forwarding.
 */
@Data
@Builder
public class GatewayRequest {

    private String         incomingPath;
    private HttpMethod     method;
    private HttpHeaders    headers;
    private byte[]         body;
    private RouteDefinition route;
    private String         clientIp;
    private String         idempotencyKey;  // value of Idempotency-Key header, if present
}
