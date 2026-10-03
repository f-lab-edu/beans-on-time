package com.bluetoya.beansontime.payment.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record PaymentApproval(
    PaymentId paymentId,
    String transactionId,
    LocalDateTime approvedAt,
    Phase phase,
    LocalDateTime applicationStartedAt,
    String cancelKey,
    LocalDateTime cancelRequestedAt,
    String cancelTransactionId,
    LocalDateTime cancelledAt) {
  public enum Phase {
    READY,
    APPLYING,
    REVIEW,
    CANCEL_PENDING,
    CANCELLED
  }

  public PaymentApproval {
    Objects.requireNonNull(paymentId);
    Objects.requireNonNull(approvedAt);
    Objects.requireNonNull(phase);
    if (transactionId == null || transactionId.isBlank())
      throw new IllegalArgumentException("승인 거래가 필요합니다.");
    if (phase == Phase.APPLYING && applicationStartedAt == null)
      throw new IllegalArgumentException("내부 반영 시작 시각이 필요합니다.");
    boolean cancelling = phase == Phase.CANCEL_PENDING || phase == Phase.CANCELLED;
    if (cancelling != (cancelKey != null)
        || cancelling != (cancelRequestedAt != null)
        || (cancelKey != null && cancelKey.isBlank()))
      throw new IllegalArgumentException("취소 결정 정보가 일치하지 않습니다.");
    if ((phase == Phase.CANCELLED) != (cancelTransactionId != null)
        || (phase == Phase.CANCELLED) != (cancelledAt != null)
        || (cancelTransactionId != null && cancelTransactionId.isBlank()))
      throw new IllegalArgumentException("취소 결과 정보가 일치하지 않습니다.");
  }

  public static PaymentApproval observed(PaymentId id, String transactionId, LocalDateTime at) {
    return new PaymentApproval(id, transactionId, at, Phase.READY, null, null, null, null, null);
  }

  public PaymentApproval beginApplication(LocalDateTime at) {
    if (phase != Phase.READY) throw new IllegalStateException("이미 반영을 시도한 승인입니다.");
    return new PaymentApproval(
        paymentId, transactionId, approvedAt, Phase.APPLYING, at, null, null, null, null);
  }

  public PaymentApproval requireReview() {
    if (phase != Phase.READY && phase != Phase.APPLYING)
      throw new IllegalStateException("검토 대기로 전환할 수 없습니다.");
    return new PaymentApproval(
        paymentId,
        transactionId,
        approvedAt,
        Phase.REVIEW,
        applicationStartedAt,
        null,
        null,
        null,
        null);
  }

  public PaymentApproval decideCancellation(LocalDateTime at) {
    if (phase != Phase.APPLYING) throw new IllegalStateException("내부 반영 확인이 필요합니다.");
    return new PaymentApproval(
        paymentId,
        transactionId,
        approvedAt,
        Phase.CANCEL_PENDING,
        applicationStartedAt,
        "cancel_" + UUID.randomUUID(),
        at,
        null,
        null);
  }

  public PaymentApproval cancelled(String transaction, LocalDateTime at) {
    if (phase != Phase.CANCEL_PENDING || transaction == null || transaction.isBlank())
      throw new IllegalStateException("취소 결과가 올바르지 않습니다.");
    return new PaymentApproval(
        paymentId,
        transactionId,
        approvedAt,
        Phase.CANCELLED,
        applicationStartedAt,
        cancelKey,
        cancelRequestedAt,
        transaction,
        Objects.requireNonNull(at));
  }
}
