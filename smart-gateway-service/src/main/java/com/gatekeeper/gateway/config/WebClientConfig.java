package com.gatekeeper.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient used by RequestForwarder and RouteRegistryLoader.
 * Connection timeout is set conservatively — per-route timeouts are
 * enforced by RequestForwarder using reactor's .timeout() operator.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
