package com.bluetoya.beansontime.payment.application.port.in;

import java.time.LocalDateTime;

public record PaymentResult(
    long paymentId,
    long billingId,
    int amount,
    String status,
    String transactionId,
    LocalDateTime attemptedAt) {}
