package com.bluetoya.beansontime.billing.adapter.in.web;

import com.bluetoya.beansontime.billing.adapter.in.web.response.BillingCheckoutResponse;
import com.bluetoya.beansontime.billing.adapter.in.web.response.PreparedBillingResponse;
import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.application.port.in.GetBillingCheckoutQuery;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingCommand;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingUseCase;
import com.bluetoya.beansontime.billing.application.port.in.PreparedBillingDetail;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BillingController {
  private final PrepareReactivationBillingUseCase prepareReactivationBillingUseCase;
  private final GetBillingCheckoutQuery getBillingCheckoutQuery;

  @PostMapping("/subscriptions/{subscriptionId}/reactivation-billing")
  PreparedBillingResponse prepare(@PathVariable UUID subscriptionId) {
    PreparedBillingDetail detail =
        prepareReactivationBillingUseCase.prepare(
            new PrepareReactivationBillingCommand(new SubscriptionId(subscriptionId)));
    return new PreparedBillingResponse(
        detail.billingId(),
        detail.amount(),
        detail.billingDate(),
        detail.status(),
        detail.createdAt());
  }

  @GetMapping("/billings/{billingId}/checkout")
  BillingCheckoutResponse checkout(
      @PathVariable @Positive(message = "청구 ID는 0보다 커야 합니다.") long billingId) {
    return toResponse(getBillingCheckoutQuery.find(new BillingId(billingId)));
  }

  private BillingCheckoutResponse toResponse(BillingCheckoutDetail detail) {
    return new BillingCheckoutResponse(
        new BillingCheckoutResponse.SubscriptionResponse(
            detail.subscription().subscriptionId(), detail.subscription().lifecycleStatus()),
        new BillingCheckoutResponse.ProductResponse(
            detail.product().productId(), detail.product().name()),
        new BillingCheckoutResponse.BillingResponse(
            detail.billing().billingId(),
            detail.billing().amount(),
            detail.billing().billingDate(),
            detail.billing().status()));
  }
}
