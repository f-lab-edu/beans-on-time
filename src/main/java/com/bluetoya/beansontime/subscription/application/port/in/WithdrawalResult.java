package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.Withdrawal;
import java.time.LocalDateTime;
import java.util.*;

public record WithdrawalResult(
    UUID subscriptionId,
    LocalDateTime requestedAt,
    Long selectedPaymentId,
    Withdrawal.Decision decision,
    List<RefundInfo> refunds) {
  public WithdrawalResult {
    refunds = List.copyOf(refunds);
  }

  public record RefundInfo(
      UUID refundId, long paymentId, int amount, String status, LocalDateTime completedAt) {}
}
