package com.bluetoya.beansontime.payment.adapter.in.web;

import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;

public record PaymentResponse(
    long paymentId,
    long billingId,
    int amount,
    String status,
    String transactionId,
    LocalDateTime attemptedAt) {
  static ResponseEntity<PaymentResponse> toResponse(PaymentResult result) {
    var response =
        new PaymentResponse(
            result.paymentId(),
            result.billingId(),
            result.amount(),
            result.status(),
            result.transactionId(),
            result.attemptedAt());
    return ResponseEntity.status(
            ("PROCESSING".equals(result.status()) || "CANCEL_PENDING".equals(result.status()))
                ? 202
                : 200)
        .body(response);
  }
}
