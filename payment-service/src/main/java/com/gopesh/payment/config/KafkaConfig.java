package com.gopesh.payment.config;

import com.gopesh.payment.event.PaymentCreatedEvent;
import com.gopesh.payment.event.FraudDecisionEvent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // --------------------------------------------------
    // COMMON KAFKA SECURITY CONFIGURATION
    // --------------------------------------------------

    private void addKafkaSecurityProperties(Map<String, Object> props) {

        props.put(
            "security.protocol",
            "SASL_SSL"
        );

        props.put(
            "sasl.mechanism",
            "SCRAM-SHA-256"
        );

        props.put(
            "sasl.jaas.config",
            "org.apache.kafka.common.security.scram.ScramLoginModule required " +
            "username=\"" + System.getenv("KAFKA_SASL_USERNAME") + "\" " +
            "password=\"" + System.getenv("KAFKA_SASL_PASSWORD") + "\";"
        );
    }

    // --------------------------------------------------
    // KAFKA TOPICS
    // --------------------------------------------------

    @Bean
    public NewTopic paymentCreatedTopic() {
        return new NewTopic(
            "payment-created",
            3,
            (short) 1
        );
    }

    @Bean
    public NewTopic fraudDecisionTopic() {
        return new NewTopic(
            "fraud-decision",
            3,
            (short) 1
        );
    }

    @Bean
    public NewTopic fraudDecisionDltTopic() {
        return new NewTopic(
            "fraud-decision.DLT",
            3,
            (short) 1
        );
    }

    // --------------------------------------------------
    // KAFKA CONSUMER
    // --------------------------------------------------

    @Bean
    public ConsumerFactory<String, FraudDecisionEvent> consumerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
            org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers
        );

        props.put(
            org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG,
            "payment-service"
        );

        props.put(
            org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class
        );

        props.put(
            org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JsonDeserializer.class
        );

        addKafkaSecurityProperties(props);

        JsonDeserializer<FraudDecisionEvent> deserializer =
            new JsonDeserializer<>(FraudDecisionEvent.class);

        deserializer.addTrustedPackages(
            "com.gopesh.payment.event"
        );

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            deserializer
        );
    }

    // --------------------------------------------------
    // FRAUD DECISION PRODUCER
    // --------------------------------------------------

    @Bean
    public ProducerFactory<String, FraudDecisionEvent> producerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
            org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers
        );

        props.put(
            org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
        );

        props.put(
            org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class
        );

        addKafkaSecurityProperties(props);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate(
        ProducerFactory<String, FraudDecisionEvent> producerFactory
    ) {

        return new KafkaTemplate<>(producerFactory);
    }

    // --------------------------------------------------
    // LISTENER + RETRY + DLT
    // --------------------------------------------------

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudDecisionEvent>
    kafkaListenerContainerFactory(
        ConsumerFactory<String, FraudDecisionEvent> consumerFactory,
        KafkaTemplate<String, FraudDecisionEvent> kafkaTemplate
    ) {

        ConcurrentKafkaListenerContainerFactory<String, FraudDecisionEvent>
            factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                    new TopicPartition(
                        record.topic() + ".DLT",
                        record.partition()
                    )
            );

        DefaultErrorHandler errorHandler =
            new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 3L)
            );

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // --------------------------------------------------
    // PAYMENT CREATED PRODUCER
    // --------------------------------------------------

    @Bean
    public ProducerFactory<String, PaymentCreatedEvent> paymentProducerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
            org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers
        );

        props.put(
            org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class
        );

        props.put(
            org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class
        );

        addKafkaSecurityProperties(props);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, PaymentCreatedEvent> paymentKafkaTemplate(
        ProducerFactory<String, PaymentCreatedEvent> paymentProducerFactory
    ) {

        return new KafkaTemplate<>(paymentProducerFactory);
    }
}
