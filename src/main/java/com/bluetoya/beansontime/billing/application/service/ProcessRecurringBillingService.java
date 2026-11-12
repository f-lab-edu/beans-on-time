package com.bluetoya.beansontime.billing.application.service;

import com.bluetoya.beansontime.billing.application.port.out.*;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.application.service.PaymentAttemptSubmission;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.*;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/** 정기결제 실행 서비스가 구독 한 건의 청구·결제를 시작할 때 사용하는 내부 협력 객체다. */
@RequiredArgsConstructor
public class ProcessRecurringBillingService {
  private final LoadSubscriptionPort subscriptions;
  private final LoadProductPort products;
  private final FindRecurringBillingPort recurring;
  private final FindPendingBillingPort pending;
  private final SaveBillingPort saveBillings;
  private final BillingExecutionPort execution;
  private final ExistsPaymentAttemptPort attempts;
  private final SavePaymentPort savePayments;
  private final PaymentAttemptSubmission submission;
  private final Clock clock;

  public void process(SubscriptionId id, LocalDate dueDate, LocalDate businessDate) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(dueDate);
    Objects.requireNonNull(businessDate);
    if (businessDate.isAfter(LocalDate.now(clock)))
      throw new IllegalArgumentException("미래 기준일로 청구할 수 없습니다.");
    Payment payment =
        execution.execute(
            id,
            () -> {
              var subscription =
                  subscriptions
                      .load(id)
                      .orElseThrow(
                          () ->
                              new IllegalStateException(
                                  "정기결제 대상으로 조회한 구독이 존재하지 않습니다: subscriptionId=" + id.value()));
              // 조회 이후 갱신·일시정지·공급 변경이 있었으면 잠금 안에서 다시 판단한다.
              if (!subscription.isRecurringBillingDue(businessDate)
                  || !dueDate.equals(subscription.getNextBillingDate())) return null;
              var product =
                  products
                      .load(subscription.getProductId())
                      .orElseThrow(
                          () ->
                              new IllegalStateException(
                                  "정기결제 구독이 참조하는 상품이 존재하지 않습니다: subscriptionId="
                                      + id.value()
                                      + ", productId="
                                      + subscription.getProductId().id()));
              if (!product.isSubscribable()) return null;
              var existing = recurring.findRecurring(id, dueDate);
              Billing billing;
              if (existing.isPresent()) {
                billing = existing.get();
                if (billing.getStatus() != BillingStatus.PENDING
                    || attempts.hasAttempt(billing.getId())) return null;
              } else {
                // 다른 미완료 청구를 새 정기 청구로 우회하지 않는다.
                if (pending.findPending(id).isPresent()) return null;
                billing =
                    Billing.recurring(
                        subscription.getCustomerId(),
                        id,
                        product.getId(),
                        product.getBasePrice(),
                        dueDate,
                        LocalDateTime.now(clock));
                saveBillings.saveNew(billing);
              }
              var started =
                  Payment.start(billing.getId(), billing.getAmount(), LocalDateTime.now(clock));
              savePayments.saveNew(started);
              return started;
            });
    // 청구와 시도를 커밋한 뒤 PG를 호출한다. 기존 미완료 시도는 결과 확인 작업이 담당한다.
    if (payment != null) submission.submit(payment);
  }
}
