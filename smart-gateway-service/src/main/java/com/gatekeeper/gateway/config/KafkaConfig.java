package com.gatekeeper.gateway.config;

import com.gatekeeper.gateway.kafka.AccessEvent;
import com.gatekeeper.gateway.kafka.RouteChangeEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Kafka consumer (route-change events) and producer (access events)
 * configuration for smart-gateway-service.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ── Consumer (RouteChangeEvent) ──────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, RouteChangeEvent> routeChangeConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG,           groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,  "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            JsonDeserializer.TRUSTED_PACKAGES, "com.gatekeeper.*",
            JsonDeserializer.VALUE_DEFAULT_TYPE, RouteChangeEvent.class.getName()
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RouteChangeEvent>
            kafkaListenerContainerFactory(
                ConsumerFactory<String, RouteChangeEvent> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, RouteChangeEvent>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    // ── Producer (AccessEvent) ───────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, AccessEvent> accessEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,       StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,     JsonSerializer.class
        ));
    }

    @Bean
    public KafkaTemplate<String, AccessEvent> accessEventKafkaTemplate(
            ProducerFactory<String, AccessEvent> factory) {
        return new KafkaTemplate<>(factory);
    }
}
