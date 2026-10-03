package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.billing.application.port.out.*;
import com.bluetoya.beansontime.payment.application.exception.PaymentCommitUncertainException;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.exception.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/** 승인 증거·내부 반영 시도·보상 결정을 각각 커밋하여 중단된 처리를 복구한다. */
@RequiredArgsConstructor
public class RecoverablePaymentCompletionService implements PaymentCompletion {
  private final LoadPaymentPort payments;
  private final SavePaymentPort savePayments;
  private final LoadBillingPort billings;
  private final SaveBillingPort saveBillings;
  private final LoadSubscriptionPort subscriptions;
  private final BillingExecutionPort execution;
  private final PaymentApprovalPort approvals;
  private final PaymentCommitPort commits;
  private final CancelGatewayPaymentPort cancellations;
  private final PaymentCompletionService completion;
  private final Clock clock;

  @Override
  public Payment complete(PaymentId id, PaymentGatewayResult result) {
    if (!result.successful()) {
      return locked(
          id, () -> approvals.load(id).isPresent() ? payment(id) : completion.complete(id, result));
    }
    locked(
        id,
        () -> {
          var payment = payment(id);
          if (payment.getStatus() != PaymentStatus.PROCESSING) return null;
          var existing = approvals.load(id);
          if (existing.isEmpty())
            approvals.saveNew(
                PaymentApproval.observed(id, result.transactionId(), result.completedAt()));
          else if (!existing.get().transactionId().equals(result.transactionId()))
            throw new IllegalStateException("서로 다른 승인 증거입니다.");
          return null;
        });
    recover(id);
    return payment(id);
  }

  @Override
  public boolean recover(PaymentId id) {
    if (approvals.load(id).isEmpty()) return false;
    boolean apply =
        locked(
            id,
            () -> {
              var payment = payment(id);
              var approval = approvals.load(id).orElseThrow();
              if (payment.getStatus() != PaymentStatus.PROCESSING
                  || approval.phase() == PaymentApproval.Phase.REVIEW) return false;
              if (approval.phase() == PaymentApproval.Phase.READY) {
                if (!validForApplication(approval)) return false;
                approvals.save(approval.beginApplication(LocalDateTime.now(clock)));
                return true;
              }
              // 정상 실행 중인 다른 요청을 취소하지 않는다. 잠금 안에서 커밋 결과를 다시 확인한다.
              if (approval.phase() == PaymentApproval.Phase.APPLYING
                  && !LocalDateTime.now(clock)
                      .isBefore(approval.applicationStartedAt().plusMinutes(2)))
                decideCancellation(approval);
              return false;
            });
    if (apply) {
      try {
        commits.commit(
            () ->
                locked(
                    id,
                    () -> {
                      var approval = approvals.load(id).orElseThrow();
                      if (payment(id).getStatus() == PaymentStatus.PROCESSING
                          && approval.phase() == PaymentApproval.Phase.APPLYING
                          && validForApplication(approval))
                        completion.complete(
                            id,
                            PaymentGatewayResult.approved(
                                approval.transactionId(), approval.approvedAt()));
                      return null;
                    }));
      } catch (PaymentCommitUncertainException exception) {
        // 이 조회도 실패하면 APPLYING 기록을 남긴 채 다음 복구에 맡긴다.
        locked(
            id,
            () -> {
              if (payment(id).getStatus() == PaymentStatus.PROCESSING)
                decideCancellation(approvals.load(id).orElseThrow());
              return null;
            });
      }
    }
    var payment = payment(id);
    if (payment.getStatus() == PaymentStatus.CANCEL_PENDING) {
      var approval = approvals.load(id).orElseThrow();
      cancellations
          .cancelOrFind(payment, approval)
          .ifPresent(
              receipt ->
                  locked(
                      id,
                      () -> {
                        var current = payment(id);
                        if (current.getStatus() != PaymentStatus.CANCEL_PENDING) return null;
                        var latest = approvals.load(id).orElseThrow();
                        var billing = billings.load(current.getBillingId()).orElseThrow();
                        approvals.save(
                            latest.cancelled(receipt.transactionId(), receipt.cancelledAt()));
                        current.completeCompensation();
                        billing.cancelAfterCompensation();
                        savePayments.save(current);
                        saveBillings.save(billing);
                        return null;
                      }));
    }
    return true;
  }

  private void decideCancellation(PaymentApproval approval) {
    if (approval.phase() != PaymentApproval.Phase.APPLYING || !validForApplication(approval))
      return;
    var payment = payment(approval.paymentId());
    payment.requestCompensation(approval.transactionId());
    approvals.save(approval.decideCancellation(LocalDateTime.now(clock)));
    savePayments.save(payment);
  }

  private boolean validForApplication(PaymentApproval approval) {
    var billing = billings.load(payment(approval.paymentId()).getBillingId()).orElseThrow();
    var subscription = subscriptions.load(billing.getSubscriptionId()).orElseThrow();
    try {
      subscription.validatePaidReactivation(approval.approvedAt().toLocalDate());
      return true;
    } catch (InvalidSubscriptionStateChangeException
        | InvalidSubscriptionPeriodStateException
        | InvalidSubscriptionResumeDateException exception) {
      approvals.save(approval.requireReview());
      return false;
    }
  }

  private Payment payment(PaymentId id) {
    return payments.load(id).orElseThrow();
  }

  private <T> T locked(PaymentId id, Supplier<T> action) {
    var billing = billings.load(payment(id).getBillingId()).orElseThrow();
    return execution.execute(billing.getSubscriptionId(), action);
  }
}
