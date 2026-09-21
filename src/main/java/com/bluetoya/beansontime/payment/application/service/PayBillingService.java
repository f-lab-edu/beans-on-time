package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.billing.application.exception.BillingAlreadyPaidException;
import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingStatus;
import com.bluetoya.beansontime.payment.application.exception.PaymentAlreadyAttemptedException;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingCommand;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingUseCase;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import com.bluetoya.beansontime.payment.application.port.out.ExistsPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGateway;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayRequest;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.application.port.out.SavePaymentPort;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentStatus;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayBillingService implements PayBillingUseCase {
  private final OwnedBillingLoader ownedBillingLoader;
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final ExistsPaymentPort existsPaymentPort;
  private final PaymentGateway paymentGateway;
  private final SavePaymentPort savePaymentPort;
  private final SaveBillingPort saveBillingPort;
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final Clock clock;

  @Override
  public PaymentResult pay(PayBillingCommand command) {
    Billing billing = ownedBillingLoader.load(command.billingId());
    if (billing.getStatus() != BillingStatus.PENDING) {
      throw new BillingAlreadyPaidException("이미 결제가 완료된 청구입니다.");
    }
    if (existsPaymentPort.existsByBillingId(billing.getId())) {
      throw new PaymentAlreadyAttemptedException("이미 결제를 시도한 청구입니다.");
    }

    Subscription subscription =
        loadSubscriptionPort
            .load(billing.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("청구의 구독이 존재하지 않습니다."));
    if (!subscription.isPaidReactivationTarget()) {
      throw new ReactivationBillingNotAllowedException("현재 구독 상태에서는 재활성화 결제를 진행할 수 없습니다.");
    }

    PaymentGatewayResult gatewayResult =
        paymentGateway.pay(new PaymentGatewayRequest(billing.getId(), billing.getAmount()));
    LocalDateTime attemptedAt = LocalDateTime.now(clock);
    Payment payment = createPayment(billing, gatewayResult, attemptedAt);
    savePaymentPort.save(payment);

    if (payment.getStatus() == PaymentStatus.FAILED) {
      subscription.addSuspensionReason(SubscriptionSuspensionReason.PAYMENT_FAILED);
      saveSubscriptionPort.save(subscription);
      return toResult(payment);
    }

    billing.markPaid();
    subscription.reactivateAfterPayment(attemptedAt.toLocalDate());
    subscription.removeSuspensionReason(SubscriptionSuspensionReason.PAYMENT_FAILED);
    saveBillingPort.save(billing);
    saveSubscriptionPort.save(subscription);
    return toResult(payment);
  }

  private Payment createPayment(
      Billing billing, PaymentGatewayResult gatewayResult, LocalDateTime attemptedAt) {
    if (gatewayResult.successful()) {
      return Payment.succeeded(
          billing.getId(), billing.getAmount(), gatewayResult.transactionId(), attemptedAt);
    }
    return Payment.failed(billing.getId(), billing.getAmount(), attemptedAt);
  }

  private PaymentResult toResult(Payment payment) {
    return new PaymentResult(
        payment.getId().value(),
        payment.getBillingId().value(),
        payment.getAmount().price(),
        payment.getStatus().name(),
        payment.getTransactionId(),
        payment.getAttemptedAt());
  }
}
