package com.gatekeeper.gateway.forwarder;

import com.gatekeeper.gateway.model.GatewayRequest;
import com.gatekeeper.gateway.model.RouteDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Forwards the incoming request to the configured backend target.
 *
 * Responsibilities:
 *   - Build the target URL from the route's targetBaseUrl + targetPathPrefix + incomingPath
 *   - Apply header rules (ADD/REMOVE) before forwarding
 *   - Enforce the route's response timeout
 *   - Return the backend's response as-is to the caller
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestForwarder {

    private final WebClient.Builder webClientBuilder;

    public Mono<ResponseEntity<byte[]>> forward(GatewayRequest gatewayRequest) {
        RouteDefinition route = gatewayRequest.getRoute();

        String targetUrl = buildTargetUrl(
                route.getTargetBaseUrl(),
                route.getTargetPathPrefix(),
                gatewayRequest.getIncomingPath()
        );

        HttpHeaders headers = applyHeaderRules(
                new HttpHeaders(gatewayRequest.getHeaders()),
                route
        );

        log.debug("Forwarding {} {} → {}", gatewayRequest.getMethod(), gatewayRequest.getIncomingPath(), targetUrl);

        return webClientBuilder.build()
                .method(gatewayRequest.getMethod())
                .uri(targetUrl)
                .headers(h -> h.addAll(headers))
                .bodyValue(gatewayRequest.getBody() != null ? gatewayRequest.getBody() : new byte[0])
                .retrieve()
                .toEntity(byte[].class)
                .timeout(Duration.ofMillis(route.getResponseTimeoutMs()))
                .onErrorResume(ex -> {
                    log.error("Backend request failed for {}: {}", targetUrl, ex.getMessage());
                    return Mono.just(ResponseEntity.status(502).body(
                            ("Backend unavailable: " + ex.getMessage()).getBytes()
                    ));
                });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildTargetUrl(String baseUrl, String pathPrefix, String incomingPath) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (pathPrefix != null && !pathPrefix.isBlank()) {
            url.append(pathPrefix);
        }
        url.append(incomingPath);
        return url.toString();
    }

    /**
     * Applies the route's header rules:
     *   ADD    → injects the header (overwrites if already present)
     *   REMOVE → strips the header before sending to the backend
     */
    private HttpHeaders applyHeaderRules(HttpHeaders headers, RouteDefinition route) {
        if (route.getHeaderRules() == null || route.getHeaderRules().isEmpty()) {
            return headers;
        }
        for (RouteDefinition.HeaderRule rule : route.getHeaderRules()) {
            if ("ADD".equalsIgnoreCase(rule.getAction())) {
                headers.set(rule.getHeaderName(), rule.getHeaderValue());
            } else if ("REMOVE".equalsIgnoreCase(rule.getAction())) {
                headers.remove(rule.getHeaderName());
            }
        }
        return headers;
    }
}
