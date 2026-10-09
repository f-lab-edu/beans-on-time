package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.billing.application.port.out.LoadBillingPort;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.BillingPurpose;
import com.bluetoya.beansontime.payment.application.port.out.LoadPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.application.port.out.SavePaymentPort;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.payment.domain.PaymentStatus;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 같은 결제의 동기 응답과 조회 결과를 동일 실행 경계에서 반영한다. */
@Service
@RequiredArgsConstructor
public class PaymentCompletionService implements PaymentCompletion {
  private final LoadPaymentPort loadPaymentPort;
  private final LoadBillingPort loadBillingPort;
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SavePaymentPort savePaymentPort;
  private final SaveBillingPort saveBillingPort;
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final BillingExecutionPort billingExecutionPort;
  private final Clock clock;
  private final com.bluetoya.beansontime.refund.application.port.out.RefundStore refunds;

  public Payment complete(PaymentId paymentId, PaymentGatewayResult result) {
    var initialPayment = loadPaymentPort.load(paymentId).orElseThrow();
    var initialBilling = loadBillingPort.load(initialPayment.getBillingId()).orElseThrow();
    billingExecutionPort.execute(
        initialBilling.getSubscriptionId(),
        () -> {
          var payment = loadPaymentPort.load(paymentId).orElseThrow();
          var billing = loadBillingPort.load(payment.getBillingId()).orElseThrow();
          // 이미 반영된 결과는 이용 기간을 다시 변경하지 않는다.
          if (payment.getStatus() != PaymentStatus.PROCESSING) {
            return null;
          }
          var subscription =
              loadSubscriptionPort
                  .load(billing.getSubscriptionId())
                  .orElseThrow(() -> new SubscriptionNotFoundException("청구의 구독이 존재하지 않습니다."));
          if (result.successful()) {
            // 새 철회 API로 철회된 구독의 진행 중 승인은 이용권을 열지 않고 반환한다.
            if (subscription.getWithdrawnAt() != null) {
              payment.succeed(result.transactionId(), result.completedAt());
              billing.markPaid();
              if (refunds.find(paymentId).isEmpty())
                refunds.saveNew(
                    com.bluetoya.beansontime.refund.domain.Refund.request(
                        paymentId,
                        payment.getAmount(),
                        result.transactionId(),
                        result.completedAt(),
                        LocalDateTime.now(clock)));
              savePaymentPort.save(payment);
              saveBillingPort.save(billing);
              return null;
            }
            // 기존 취소와 그 밖의 구독 상태 충돌은 확인 대상으로 남긴다.
            if (billing.getPurpose() == BillingPurpose.RECURRING)
              subscription.renewAfterPayment(
                  billing.getBillingDate(), result.completedAt().toLocalDate());
            else subscription.reactivateAfterPayment(result.completedAt().toLocalDate());
            payment.succeed(result.transactionId(), result.completedAt());
            billing.markPaid();
          } else {
            payment.fail();
            if (billing.getPurpose() == BillingPurpose.RECURRING)
              subscription.recordRecurringPaymentDeclined(billing.getBillingDate());
            else subscription.recordReactivationPaymentDeclined();
            billing.expireIfDue(LocalDateTime.now(clock), false);
          }
          savePaymentPort.save(payment);
          saveBillingPort.save(billing);
          saveSubscriptionPort.save(subscription);
          return null;
        });
    return loadPaymentPort.load(paymentId).orElseThrow();
  }
}
