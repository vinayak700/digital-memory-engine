package com.memory.context.engine.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for event streaming.
 * Configured for Confluent Cloud with SASL_SSL authentication.
 */
@Configuration
public class KafkaConfig {

        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        @Value("${spring.kafka.properties.security.protocol:SASL_SSL}")
        private String securityProtocol;

        @Value("${spring.kafka.properties.sasl.mechanism:PLAIN}")
        private String saslMechanism;

        @Value("${spring.kafka.properties.sasl.jaas.config:}")
        private String saslJaasConfig;

        @Bean
        @SuppressWarnings("deprecation")
        public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
                Map<String, Object> configProps = new HashMap<>();

                // Basic producer config
                configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
                configProps.put(ProducerConfig.ACKS_CONFIG, "all");
                configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
                configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

                // SASL/SSL security config for Confluent Cloud
                configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
                configProps.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
                configProps.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);

                // SSL endpoint identification
                configProps.put("ssl.endpoint.identification.algorithm", "https");

                DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);
                factory.setValueSerializer(new JsonSerializer<>(objectMapper));

                return factory;
        }

        @Bean
        public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
                return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        public org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
                        org.springframework.kafka.core.ConsumerFactory<String, Object> consumerFactory) {

                org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, Object> factory = new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(consumerFactory);

                // FIX 1: Concurrency - Run 3 consumers in parallel to prevent bottlenecks
                factory.setConcurrency(3);

                // FIX 2: Resilience - Handle poison pills without infinite loops
                // Retry 3 times with 1 second backoff, then log and skip
                org.springframework.util.backoff.FixedBackOff backOff = new org.springframework.util.backoff.FixedBackOff(
                                1000L,
                                3);

                org.springframework.kafka.listener.DefaultErrorHandler errorHandler = new org.springframework.kafka.listener.DefaultErrorHandler(
                                backOff);

                factory.setCommonErrorHandler(errorHandler);

                return factory;
        }

        @Bean
        public org.springframework.kafka.core.ConsumerFactory<String, Object> consumerFactory(
                        ObjectMapper objectMapper) {
                Map<String, Object> props = new HashMap<>();
                props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                org.apache.kafka.common.serialization.StringDeserializer.class);
                props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);
                props.put(org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                                org.springframework.kafka.support.serializer.JsonDeserializer.class);
                props.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");

                // SASL/SSL Config
                props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
                props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
                props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
                props.put("ssl.endpoint.identification.algorithm", "https");

                return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(props,
                                new org.apache.kafka.common.serialization.StringDeserializer(),
                                new org.springframework.kafka.support.serializer.JsonDeserializer<>(Object.class,
                                                objectMapper));
        }

        /**
         * Topic names for memory events.
         */
        public static class Topics {
                public static final String MEMORY_EVENTS = "memory-events";
                public static final String MEMORY_AUDIT = "memory-audit";

                private Topics() {
                }
        }
}
