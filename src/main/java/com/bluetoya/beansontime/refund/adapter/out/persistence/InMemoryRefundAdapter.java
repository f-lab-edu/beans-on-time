package com.bluetoya.beansontime.refund.adapter.out.persistence;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import com.bluetoya.beansontime.refund.domain.Refund;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("in-memory")
@lombok.RequiredArgsConstructor
public class InMemoryRefundAdapter implements RefundStore {
  private final com.bluetoya.beansontime.payment.application.port.out.LoadPaymentPort payments;
  private final com.bluetoya.beansontime.billing.application.port.out.LoadBillingPort billings;
  private final Map<PaymentId, Refund> refunds = new ConcurrentHashMap<>();

  public void saveNew(Refund refund) {
    if (refunds.putIfAbsent(refund.paymentId(), refund) != null)
      throw new IllegalStateException("이미 환불 요청이 있습니다.");
  }

  public void save(Refund refund) {
    if (refunds.replace(refund.paymentId(), refund) == null)
      throw new IllegalStateException("저장할 환불 요청이 없습니다.");
  }

  public Optional<Refund> find(PaymentId id) {
    return Optional.ofNullable(refunds.get(id));
  }

  public List<Refund> findBySubscription(
      com.bluetoya.beansontime.subscription.domain.SubscriptionId id) {
    return refunds.values().stream()
        .filter(
            r ->
                billings
                    .load(payments.load(r.paymentId()).orElseThrow().getBillingId())
                    .orElseThrow()
                    .getSubscriptionId()
                    .equals(id))
        .sorted(
            java.util.Comparator.comparing(Refund::requestedAt)
                .thenComparing(r -> r.id().toString()))
        .toList();
  }

  public List<PaymentId> findPending() {
    return refunds.values().stream().filter(r -> !r.isCompleted()).map(Refund::paymentId).toList();
  }
}
