package com.gopesh.payment.controller;

import java.util.List;

import com.gopesh.payment.dto.PaymentRequest;
import com.gopesh.payment.entity.Payment;
import com.gopesh.payment.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        Payment payment =
                paymentService.createPayment(request, idempotencyKey);

        return ResponseEntity.ok(payment);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable Long id) {

        return paymentService.getPayment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {

        return ResponseEntity.ok(
                paymentService.getAllPayments()
        );
    }
}