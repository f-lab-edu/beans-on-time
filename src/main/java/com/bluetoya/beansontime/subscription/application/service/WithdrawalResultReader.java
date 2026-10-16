package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import com.bluetoya.beansontime.subscription.application.port.in.WithdrawalResult;
import com.bluetoya.beansontime.subscription.domain.Withdrawal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawalResultReader {
  private final RefundStore refunds;

  public WithdrawalResult read(Withdrawal w) {
    return new WithdrawalResult(
        w.subscriptionId().value(),
        w.requestedAt(),
        w.paymentId() == null ? null : w.paymentId().value(),
        w.decision(),
        refunds.findBySubscription(w.subscriptionId()).stream()
            .map(
                r ->
                    new WithdrawalResult.RefundInfo(
                        r.id(),
                        r.paymentId().value(),
                        r.amount().price(),
                        r.isCompleted() ? "SUCCEEDED" : "PENDING",
                        r.completedAt()))
            .toList());
  }
}
