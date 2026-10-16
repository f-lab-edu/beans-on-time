package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.billing.application.exception.BillingAlreadyPaidException;
import com.bluetoya.beansontime.billing.application.exception.BillingExpiredException;
import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingStatus;
import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.exception.PaymentInProgressException;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingCommand;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingUseCase;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import com.bluetoya.beansontime.payment.application.port.out.FindProcessingPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.LoadPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGateway;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayRequest;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.application.port.out.SavePaymentPort;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentAuthorization;
import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayBillingService implements PayBillingUseCase {
  private final OwnedBillingLoader ownedBillingLoader;
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final FindProcessingPaymentPort findProcessingPaymentPort;
  private final PaymentGateway paymentGateway;
  private final SavePaymentPort savePaymentPort;
  private final SaveBillingPort saveBillingPort;
  private final LoadProductPort loadProductPort;
  private final BillingExecutionPort billingExecutionPort;
  private final PaymentCompletion paymentCompletionService;
  private final Clock clock;
  private final LoadPaymentPort loadPaymentPort;

  @Override
  public PaymentResult pay(PayBillingCommand command) {
    Billing billing = ownedBillingLoader.load(command.billingId());
    Payment payment =
        billingExecutionPort.execute(
            billing.getSubscriptionId(),
            () -> start(ownedBillingLoader.load(command.billingId()), command.authorization()));
    if (payment == null) {
      // 만료 상태를 커밋한 다음 충돌을 반환한다.
      throw new BillingExpiredException("만료된 청구입니다. 새 청구를 준비해 주세요.");
    }
    try {
      PaymentGatewayResult result =
          paymentGateway.pay(
              new PaymentGatewayRequest(payment.getId(), billing.getId(), billing.getAmount()));
      payment = paymentCompletionService.complete(payment.getId(), result);
    } catch (PaymentGatewayUnavailableException exception) {
      // 응답이 없다는 이유로 외부 결제를 실패로 확정하지 않는다.
    }
    return toResult(loadPaymentPort.load(payment.getId()).orElseThrow());
  }

  private Payment start(Billing billing, PaymentAuthorization authorization) {
    if (billing.getStatus() == BillingStatus.PAID) {
      throw new BillingAlreadyPaidException("이미 결제가 완료된 청구입니다.");
    }
    if (billing.getStatus() == BillingStatus.CANCELLED) {
      throw new ReactivationBillingNotAllowedException("보상 취소가 완료된 청구입니다. 새 청구를 준비해 주세요.");
    }
    var processing = findProcessingPaymentPort.findProcessing(billing.getId());
    LocalDateTime now = LocalDateTime.now(clock);
    billing.expireIfDue(now, processing.isPresent());
    saveBillingPort.save(billing);
    if (processing.isPresent()) {
      throw new PaymentInProgressException(processing.get().getId());
    }
    if (billing.getStatus() == BillingStatus.EXPIRED) {
      return null;
    }
    Subscription subscription =
        loadSubscriptionPort
            .load(billing.getSubscriptionId())
            .orElseThrow(() -> new SubscriptionNotFoundException("청구의 구독이 존재하지 않습니다."));
    if (!subscription.isPaidReactivationTarget()) {
      throw new ReactivationBillingNotAllowedException("현재 구독 상태에서는 재활성화 결제를 진행할 수 없습니다.");
    }
    var product =
        loadProductPort
            .load(billing.getProductId())
            .orElseThrow(() -> new ProductNotFoundException("청구할 상품이 존재하지 않습니다."));
    if (!product.isSubscribable()) {
      throw new ReactivationBillingNotAllowedException("현재 공급할 수 없는 상품입니다.");
    }
    // 사전 조회에 시간이 걸린 경우에도 실제 시도 생성 시각의 기한을 지킨다.
    now = LocalDateTime.now(clock);
    if (billing.isPaymentWindowClosed(now)) {
      billing.expireIfDue(now, false);
      saveBillingPort.save(billing);
      return null;
    }
    subscription.validatePaidReactivation(now.toLocalDate());
    Payment payment = Payment.start(billing.getId(), billing.getAmount(), now);
    if (authorization == null) savePaymentPort.saveNew(payment);
    else savePaymentPort.saveAuthorized(payment, authorization);
    return payment;
  }

  static PaymentResult toResult(Payment payment) {
    return new PaymentResult(
        payment.getId().value(),
        payment.getBillingId().value(),
        payment.getAmount().price(),
        payment.getStatus().name(),
        payment.getTransactionId(),
        payment.getAttemptedAt());
  }
}
