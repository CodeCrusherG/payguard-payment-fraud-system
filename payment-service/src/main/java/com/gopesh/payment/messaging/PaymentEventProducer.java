package com.gopesh.payment.messaging;

import com.gopesh.payment.event.PaymentCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {

    private static final String TOPIC = "payment-created";

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCreated(PaymentCreatedEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event.getPaymentId().toString(),
                event
        );
    }
}