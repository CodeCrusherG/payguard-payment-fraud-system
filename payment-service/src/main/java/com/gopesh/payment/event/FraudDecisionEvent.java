package com.gopesh.payment.event;

public class FraudDecisionEvent {
    private Long paymentId;
    private Long userId;
    private java.math.BigDecimal amount;
    private String merchant;
    private Double riskScore;
    private String decision;

    public FraudDecisionEvent() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public java.math.BigDecimal getAmount() {

    return amount;

}

    public void setAmount(java.math.BigDecimal amount) {

    this.amount = amount;

}
    public Long getPaymentId() {
    return paymentId;
}

public void setPaymentId(Long paymentId) {
    this.paymentId = paymentId;
}
    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

}