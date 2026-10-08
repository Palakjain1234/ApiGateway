package com.gatekeeper.analytics.config;

import com.gatekeeper.analytics.consumer.AccessEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, AccessEvent> accessEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG,                   groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,          "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,     StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,   JsonDeserializer.class,
            JsonDeserializer.TRUSTED_PACKAGES,                "com.gatekeeper.*",
            JsonDeserializer.VALUE_DEFAULT_TYPE,              AccessEvent.class.getName()
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AccessEvent>
            kafkaListenerContainerFactory(ConsumerFactory<String, AccessEvent> cf) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, AccessEvent>();
        factory.setConsumerFactory(cf);
        return factory;
    }
}
