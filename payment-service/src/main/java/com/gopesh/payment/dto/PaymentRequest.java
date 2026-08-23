package com.gopesh.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(
        value = "0.01",
        message = "Amount must be greater than 0"
    )
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Merchant is required")
    private String merchant;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }
}