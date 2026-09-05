package com.bluetoya.beansontime.payment.domain;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.product.domain.Money;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Payment {
  private final PaymentId id;
  private final BillingId billingId;
  private final Money amount;
  private final PaymentStatus status;
  private final String transactionId;
  private final LocalDateTime attemptedAt;

  private Payment(
      BillingId billingId,
      Money amount,
      PaymentStatus status,
      String transactionId,
      LocalDateTime attemptedAt) {
    this.id = PaymentId.generate();
    this.billingId = Objects.requireNonNull(billingId, "결제 청구 ID는 필수입니다.");
    this.amount = Objects.requireNonNull(amount, "결제 금액은 필수입니다.");
    this.status = Objects.requireNonNull(status, "결제 상태는 필수입니다.");
    this.transactionId = transactionId;
    this.attemptedAt = Objects.requireNonNull(attemptedAt, "결제 시도 시각은 필수입니다.");
  }

  public static Payment succeeded(
      BillingId billingId, Money amount, String transactionId, LocalDateTime attemptedAt) {
    if (transactionId == null || transactionId.isBlank()) {
      throw new IllegalArgumentException("성공한 결제의 거래 식별자는 필수입니다.");
    }

    return new Payment(billingId, amount, PaymentStatus.SUCCESS, transactionId, attemptedAt);
  }

  public static Payment failed(BillingId billingId, Money amount, LocalDateTime attemptedAt) {
    return new Payment(billingId, amount, PaymentStatus.FAILED, null, attemptedAt);
  }
}
