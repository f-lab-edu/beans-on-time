package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.payment.application.port.out.FindLatestSuccessfulPaymentPort;
import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import com.bluetoya.beansontime.refund.application.service.RefundProcessor;
import com.bluetoya.beansontime.refund.domain.Refund;
import com.bluetoya.beansontime.subscription.application.port.in.*;
import com.bluetoya.beansontime.subscription.application.port.out.*;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawSubscriptionService implements WithdrawSubscriptionUseCase {
  private final OwnedSubscriptionLoader owned;
  private final SubscriptionExecutionPort execution;
  private final SaveSubscriptionPort subscriptions;
  private final FindLatestSuccessfulPaymentPort payments;
  private final WithdrawalStore withdrawals;
  private final RefundStore refunds;
  private final RefundProcessor processor;
  private final WithdrawalResultReader results;
  private final Clock clock;

  public WithdrawalResult withdraw(SubscriptionId id) {
    owned.load(id);
    var requestedAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    var withdrawal =
        execution.execute(
            id,
            () -> {
              var subscription = owned.load(id);
              var existing = withdrawals.find(id);
              if (existing.isPresent()) return existing.get();
              var decision = decide(subscription, requestedAt);
              if (subscription.getLifecycleStatus() != SubscriptionStatus.CANCELLED) {
                // 실행 잠금을 기다리는 사이 승인 반영이 끝난 건도 지연 승인 반환에서 빠뜨리지 않는다.
                for (var late : payments.findApprovedAfterRequest(id, requestedAt)) {
                  if (refunds.find(late.getId()).isEmpty())
                    refunds.saveNew(
                        Refund.request(
                            late.getId(),
                            late.getAmount(),
                            late.getTransactionId(),
                            late.getApprovedAt(),
                            LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS)));
                }
              }
              subscription.withdraw(requestedAt);
              subscriptions.save(subscription);
              withdrawals.saveNew(decision);
              return decision;
            });
    for (var refund : refunds.findBySubscription(id)) {
      if (refund.isCompleted()) continue;
      try {
        processor.process(refund.paymentId());
      } catch (RuntimeException e) {
        log.warn("구독 철회 완료 후 환불 결과 확인 필요: paymentId={}", refund.paymentId().value(), e);
      }
    }
    return results.read(withdrawal);
  }

  private Withdrawal decide(Subscription s, LocalDateTime at) {
    if (s.getLifecycleStatus() == SubscriptionStatus.CANCELLED)
      return new Withdrawal(s.getId(), at, null, Withdrawal.Decision.PREVIOUSLY_CANCELLED);
    var latest = payments.findLatestSuccess(s.getId(), at);
    if (latest.isEmpty())
      return new Withdrawal(s.getId(), at, null, Withdrawal.Decision.NO_PAYMENT);
    var p = latest.get();
    var existing = refunds.find(p.getId());
    Withdrawal.Decision decision;
    if (existing.isPresent())
      decision =
          existing.get().isCompleted()
              ? Withdrawal.Decision.ALREADY_REFUNDED
              : Withdrawal.Decision.REFUND_REQUESTED;
    else if (p.getApprovedAt() == null || at.isBefore(p.getApprovedAt()))
      decision = Withdrawal.Decision.REVIEW_REQUIRED;
    else if (!Refund.withinWindow(p.getApprovedAt(), at))
      decision = Withdrawal.Decision.OUTSIDE_WINDOW;
    else {
      refunds.saveNew(
          Refund.request(p.getId(), p.getAmount(), p.getTransactionId(), p.getApprovedAt(), at));
      decision = Withdrawal.Decision.REFUND_REQUESTED;
    }
    return new Withdrawal(s.getId(), at, p.getId(), decision);
  }
}
