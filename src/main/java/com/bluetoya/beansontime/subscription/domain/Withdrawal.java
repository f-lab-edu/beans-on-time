package com.bluetoya.beansontime.subscription.domain;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.time.LocalDateTime;
import java.util.Objects;

/** 최초 철회에서 확정한 일반 환불 대상과 판단. 지연 승인 반환과 구분한다. */
public record Withdrawal(
    SubscriptionId subscriptionId,
    LocalDateTime requestedAt,
    PaymentId paymentId,
    Decision decision) {
  public enum Decision {
    NO_PAYMENT,
    OUTSIDE_WINDOW,
    REFUND_REQUESTED,
    ALREADY_REFUNDED,
    REVIEW_REQUIRED,
    PREVIOUSLY_CANCELLED
  }

  public Withdrawal {
    Objects.requireNonNull(subscriptionId);
    Objects.requireNonNull(requestedAt);
    Objects.requireNonNull(decision);
    if ((decision == Decision.REFUND_REQUESTED
            || decision == Decision.OUTSIDE_WINDOW
            || decision == Decision.ALREADY_REFUNDED)
        && paymentId == null) throw new IllegalArgumentException("환불 판단의 대상 결제는 필수입니다.");
  }
}
