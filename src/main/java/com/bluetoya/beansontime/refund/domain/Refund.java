package com.bluetoya.beansontime.refund.domain;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.product.domain.Money;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** 한 승인 결제의 전액 반환 요청. 미확정 결과는 같은 요청 식별자로 복구한다. */
public record Refund(
    UUID id,
    PaymentId paymentId,
    Money amount,
    String approvalTransactionId,
    LocalDateTime approvedAt,
    LocalDateTime requestedAt,
    String transactionId,
    LocalDateTime completedAt) {
  public Refund {
    Objects.requireNonNull(id);
    Objects.requireNonNull(paymentId);
    Objects.requireNonNull(amount);
    Objects.requireNonNull(approvedAt);
    Objects.requireNonNull(requestedAt);
    if (approvalTransactionId == null || approvalTransactionId.isBlank())
      throw new IllegalArgumentException("환불 대상 승인 거래는 필수입니다.");
    if ((transactionId == null) != (completedAt == null)
        || (transactionId != null && transactionId.isBlank()))
      throw new IllegalArgumentException("환불 완료 정보가 일치하지 않습니다.");
  }

  public static Refund request(
      PaymentId paymentId,
      Money amount,
      String transaction,
      LocalDateTime approvedAt,
      LocalDateTime requestedAt) {
    return new Refund(
        UUID.randomUUID(), paymentId, amount, transaction, approvedAt, requestedAt, null, null);
  }

  public boolean isCompleted() {
    return completedAt != null;
  }

  public String idempotencyKey() {
    return "refund_" + id;
  }

  public Refund complete(String transaction, LocalDateTime at) {
    Objects.requireNonNull(transaction);
    Objects.requireNonNull(at);
    if (isCompleted()) {
      if (!transactionId.equals(transaction) || !completedAt.equals(at))
        throw new IllegalStateException("기존 환불 결과와 다릅니다.");
      return this;
    }
    return new Refund(
        id, paymentId, amount, approvalTransactionId, approvedAt, requestedAt, transaction, at);
  }

  public static boolean withinWindow(LocalDateTime approvedAt, LocalDateTime withdrawnAt) {
    return approvedAt != null
        && !withdrawnAt.isBefore(approvedAt)
        && !withdrawnAt.isAfter(approvedAt.plusHours(24));
  }
}
