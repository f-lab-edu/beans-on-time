package com.bluetoya.beansontime.billing.application.service;

import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingCommand;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingUseCase;
import com.bluetoya.beansontime.billing.application.port.in.PreparedBillingDetail;
import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.billing.application.port.out.FindPendingBillingPort;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingPurpose;
import com.bluetoya.beansontime.billing.domain.BillingStatus;
import com.bluetoya.beansontime.payment.application.port.out.FindProcessingPaymentPort;
import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrepareReactivationBillingService implements PrepareReactivationBillingUseCase {
  private final OwnedSubscriptionLoader ownedSubscriptionLoader;
  private final FindPendingBillingPort findPendingBillingPort;
  private final LoadProductPort loadProductPort;
  private final SaveBillingPort saveBillingPort;
  private final Clock clock;
  private final BillingExecutionPort billingExecutionPort;
  private final FindProcessingPaymentPort findProcessingPaymentPort;

  @Override
  public PreparedBillingDetail prepare(PrepareReactivationBillingCommand command) {
    Subscription subscription = ownedSubscriptionLoader.load(command.subscriptionId());
    return billingExecutionPort.execute(
        subscription.getId(), () -> prepare(ownedSubscriptionLoader.load(subscription.getId())));
  }

  private PreparedBillingDetail prepare(Subscription subscription) {
    if (!subscription.isPaidReactivationTarget()) {
      throw new ReactivationBillingNotAllowedException(
          "일시정지 상태이고 남은 선결제 이용 기간이 없는 구독만 재활성화 청구를 준비할 수 있습니다.");
    }

    Product product =
        loadProductPort
            .load(subscription.getProductId())
            .orElseThrow(() -> new ProductNotFoundException("청구할 상품이 존재하지 않습니다."));
    if (!product.isSubscribable()) {
      throw new ReactivationBillingNotAllowedException("현재 공급할 수 없는 상품입니다.");
    }
    var pending = findPendingBillingPort.findPending(subscription.getId());
    if (pending.isPresent()) {
      Billing billing = pending.get();
      if (billing.getPurpose() != BillingPurpose.REACTIVATION)
        throw new ReactivationBillingNotAllowedException("미완료 정기 청구를 먼저 확인해야 합니다.");
      billing.expireIfDue(
          LocalDateTime.now(clock),
          findProcessingPaymentPort.findProcessing(billing.getId()).isPresent());
      saveBillingPort.save(billing);
      if (billing.getStatus() == BillingStatus.PENDING) {
        return toDetail(billing);
      }
    }
    return prepareNewBilling(subscription, product);
  }

  private PreparedBillingDetail prepareNewBilling(Subscription subscription, Product product) {
    LocalDateTime createdAt = LocalDateTime.now(clock);
    Billing billing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            subscription.getProductId(),
            product.getBasePrice(),
            createdAt.toLocalDate(),
            createdAt);

    saveBillingPort.saveNew(billing);
    return toDetail(billing);
  }

  private PreparedBillingDetail toDetail(Billing billing) {
    return new PreparedBillingDetail(
        billing.getId().value(),
        billing.getAmount().price(),
        billing.getBillingDate(),
        billing.getStatus().name(),
        billing.getCreatedAt(),
        billing.getExpiresAt());
  }
}
