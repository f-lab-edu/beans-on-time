package com.bluetoya.beansontime.payment.domain;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.exception.InvalidPaymentStateChangeException;
import com.bluetoya.beansontime.product.domain.Money;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Payment {
  private final PaymentId id;
  private final BillingId billingId;
  private final Money amount;
  private volatile PaymentStatus status;
  private String transactionId;
  private final LocalDateTime attemptedAt;
  private LocalDateTime approvedAt;

  private Payment(
      BillingId billingId,
      Money amount,
      PaymentStatus status,
      String transactionId,
      LocalDateTime attemptedAt) {
    this(PaymentId.generate(), billingId, amount, status, transactionId, attemptedAt);
  }

  private Payment(
      PaymentId id,
      BillingId billingId,
      Money amount,
      PaymentStatus status,
      String transactionId,
      LocalDateTime attemptedAt) {
    this.id = Objects.requireNonNull(id);
    this.billingId = Objects.requireNonNull(billingId, "결제 청구 ID는 필수입니다.");
    this.amount = Objects.requireNonNull(amount, "결제 금액은 필수입니다.");
    this.status = Objects.requireNonNull(status, "결제 상태는 필수입니다.");
    if (((status == PaymentStatus.SUCCESS
                || status == PaymentStatus.CANCEL_PENDING
                || status == PaymentStatus.CANCELLED)
            && (transactionId == null || transactionId.isBlank()))
        || ((status == PaymentStatus.PROCESSING || status == PaymentStatus.FAILED)
            && transactionId != null)) {
      throw new IllegalArgumentException("결제 상태와 승인 거래 식별자가 일치하지 않습니다.");
    }
    this.transactionId = transactionId;
    this.attemptedAt = Objects.requireNonNull(attemptedAt, "결제 시도 시각은 필수입니다.");
  }

  public static Payment restore(
      PaymentId id,
      BillingId billingId,
      Money amount,
      PaymentStatus status,
      String transactionId,
      LocalDateTime attemptedAt,
      LocalDateTime approvedAt) {
    var payment = new Payment(id, billingId, amount, status, transactionId, attemptedAt);
    if (approvedAt != null && status != PaymentStatus.SUCCESS)
      throw new IllegalArgumentException("성공 결제에만 승인 시각을 기록할 수 있습니다.");
    payment.approvedAt = approvedAt;
    return payment;
  }

  public static Payment start(BillingId billingId, Money amount, LocalDateTime attemptedAt) {
    return new Payment(billingId, amount, PaymentStatus.PROCESSING, null, attemptedAt);
  }

  public void succeed(String transactionId, LocalDateTime approvedAt) {
    Objects.requireNonNull(approvedAt, "승인 시각은 필수입니다.");
    if (transactionId == null || transactionId.isBlank()) {
      throw new IllegalArgumentException("성공한 결제의 거래 식별자는 필수입니다.");
    }
    if (status == PaymentStatus.SUCCESS && transactionId.equals(this.transactionId)) {
      if (!approvedAt.equals(this.approvedAt)) throw new IllegalStateException("기존 승인 시각과 다릅니다.");
      return;
    }
    requireProcessing();
    this.transactionId = transactionId;
    this.status = PaymentStatus.SUCCESS;
    this.approvedAt = approvedAt;
  }

  public void requestCompensation(String approvalTransactionId) {
    if (status == PaymentStatus.CANCEL_PENDING
        && Objects.equals(transactionId, approvalTransactionId)) return;
    requireProcessing();
    if (approvalTransactionId == null || approvalTransactionId.isBlank())
      throw new IllegalArgumentException("승인 증거가 필요합니다.");
    transactionId = approvalTransactionId;
    status = PaymentStatus.CANCEL_PENDING;
  }

  public void completeCompensation() {
    if (status == PaymentStatus.CANCELLED) return;
    if (status != PaymentStatus.CANCEL_PENDING)
      throw new InvalidPaymentStateChangeException("취소 결정이 없는 결제입니다.");
    status = PaymentStatus.CANCELLED;
  }

  public void fail() {
    if (status == PaymentStatus.FAILED) {
      return;
    }
    requireProcessing();
    this.status = PaymentStatus.FAILED;
  }

  private void requireProcessing() {
    if (status != PaymentStatus.PROCESSING) {
      throw new InvalidPaymentStateChangeException("확정된 결제 결과를 변경할 수 없습니다.");
    }
  }

  public static Payment succeeded(
      BillingId billingId,
      Money amount,
      String transactionId,
      LocalDateTime attemptedAt,
      LocalDateTime approvedAt) {
    if (transactionId == null || transactionId.isBlank()) {
      throw new IllegalArgumentException("성공한 결제의 거래 식별자는 필수입니다.");
    }

    var payment = start(billingId, amount, attemptedAt);
    payment.succeed(transactionId, approvedAt);
    return payment;
  }

  public static Payment failed(BillingId billingId, Money amount, LocalDateTime attemptedAt) {
    return new Payment(billingId, amount, PaymentStatus.FAILED, null, attemptedAt);
  }
}
