package com.gopesh.payment.service;
import java.util.Optional;
import com.gopesh.payment.dto.PaymentRequest;
import com.gopesh.payment.entity.Payment;
import com.gopesh.payment.event.PaymentCreatedEvent;
import com.gopesh.payment.messaging.PaymentEventProducer;
import com.gopesh.payment.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventProducer eventProducer) {

        this.paymentRepository = paymentRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public Payment createPayment(
            PaymentRequest request,
            String idempotencyKey) {

        // 1. Check for duplicate request
        Optional<Payment> existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        // 2. Create payment
        Payment payment = new Payment();

        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMerchant(request.getMerchant());
        payment.setStatus("PROCESSING");
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(LocalDateTime.now());

        // 3. Save to PostgreSQL
        Payment savedPayment = paymentRepository.save(payment);

        // 4. Publish event to Kafka
        PaymentCreatedEvent event =
                new PaymentCreatedEvent(
                        savedPayment.getId(),
                        savedPayment.getUserId(),
                        savedPayment.getAmount(),
                        savedPayment.getCurrency(),
                        savedPayment.getMerchant()
                );

        eventProducer.publishPaymentCreated(event);

        return savedPayment;
    }
    public Optional<Payment> getPayment(Long id) {

        return paymentRepository.findById(id);
    }

public java.util.List<Payment> getAllPayments() {

    return paymentRepository.findAll(
            org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC,
                    "id"
            )
    );
}

}