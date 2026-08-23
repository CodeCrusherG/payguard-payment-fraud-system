package com.gopesh.payment.event;

import java.math.BigDecimal;

public class PaymentCreatedEvent {

    private Long paymentId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String merchant;

    public PaymentCreatedEvent() {
    }

    public PaymentCreatedEvent(
            Long paymentId,
            Long userId,
            BigDecimal amount,
            String currency,
            String merchant) {

        this.paymentId = paymentId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.merchant = merchant;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMerchant() {
        return merchant;
    }
}