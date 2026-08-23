package com.gopesh.payment.messaging;

import com.gopesh.payment.entity.Payment;
import com.gopesh.payment.event.FraudDecisionEvent;
import com.gopesh.payment.repository.PaymentRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FraudDecisionConsumer {

    private final PaymentRepository paymentRepository;

    public FraudDecisionConsumer(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @KafkaListener(
        topics = "fraud-decision",
        groupId = "payment-service"
    )
    public void consume(FraudDecisionEvent event) {

        System.out.println(
            "Received fraud decision: " +
            event.getDecision() +
            " for user " +
            event.getUserId()
        );

        Payment payment = paymentRepository
    .findById(event.getPaymentId())
    .orElse(null);

        if (payment == null) {
            System.out.println(
                "Payment not found for fraud decision"
            );
            return;
        }

        payment.setStatus(event.getDecision());

        paymentRepository.save(payment);

        System.out.println(
            "Payment " +
            payment.getId() +
            " updated to " +
            event.getDecision()
        );
    }
}