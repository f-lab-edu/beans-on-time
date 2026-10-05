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
            // 구독 상태 충돌 시 확정을 중단하고 PROCESSING을 유지해 운영 확인 대상으로 남긴다.
            if (billing.getPurpose() == BillingPurpose.RECURRING)
              subscription.renewAfterPayment(
                  billing.getBillingDate(), result.completedAt().toLocalDate());
            else subscription.reactivateAfterPayment(result.completedAt().toLocalDate());
            payment.succeed(result.transactionId());
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
