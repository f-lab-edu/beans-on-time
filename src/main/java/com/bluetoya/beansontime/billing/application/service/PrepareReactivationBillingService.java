package com.bluetoya.beansontime.billing.application.service;

import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingCommand;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingUseCase;
import com.bluetoya.beansontime.billing.application.port.in.PreparedBillingDetail;
import com.bluetoya.beansontime.billing.application.port.out.FindPendingBillingPort;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
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

  @Override
  public PreparedBillingDetail prepare(PrepareReactivationBillingCommand command) {
    Subscription subscription = ownedSubscriptionLoader.load(command.subscriptionId());
    if (!subscription.isPaidReactivationTarget()) {
      throw new ReactivationBillingNotAllowedException(
          "일시정지 상태이고 남은 선결제 이용 기간이 없는 구독만 재활성화 청구를 준비할 수 있습니다.");
    }

    return findPendingBillingPort
        .findPending(subscription.getId())
        .map(this::toDetail)
        .orElseGet(() -> prepareNewBilling(subscription));
  }

  private PreparedBillingDetail prepareNewBilling(Subscription subscription) {
    Product product =
        loadProductPort
            .load(subscription.getProductId())
            .orElseThrow(() -> new ProductNotFoundException("청구할 상품이 존재하지 않습니다."));
    LocalDateTime createdAt = LocalDateTime.now(clock);
    Billing billing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            subscription.getProductId(),
            product.basePrice(),
            createdAt.toLocalDate(),
            createdAt);

    saveBillingPort.save(billing);
    return toDetail(billing);
  }

  private PreparedBillingDetail toDetail(Billing billing) {
    return new PreparedBillingDetail(
        billing.getId().value(),
        billing.getAmount().price(),
        billing.getBillingDate(),
        billing.getStatus().name(),
        billing.getCreatedAt());
  }
}
