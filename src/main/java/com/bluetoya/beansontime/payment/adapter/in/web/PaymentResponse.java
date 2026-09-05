package com.bluetoya.beansontime.payment.adapter.in.web;

import java.time.LocalDateTime;

public record PaymentResponse(
    long paymentId,
    long billingId,
    int amount,
    String status,
    String transactionId,
    LocalDateTime attemptedAt) {}
