package com.bluetoya.beansontime.billing.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.aspect.OwnershipAspect;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class BillingOwnershipResolverTest {

  @Test
  void independentlyAuthorizesBillingAndCheckoutResources() {
    CurrentActorProvider actorProvider = mock(CurrentActorProvider.class);
    when(actorProvider.getCurrentActor()).thenReturn(new ActorIdentity(ActorType.CUSTOMER, 42));
    Billing billing = billingOwnedBy(42);
    BillingCheckoutDetail checkout = checkoutOwnedBy(42, billing);
    OwnershipAspect aspect =
        new OwnershipAspect(
            actorProvider,
            List.of(new BillingOwnershipResolver(), new BillingCheckoutOwnershipResolver()));

    assertThatCode(() -> aspect.authorize(billing)).doesNotThrowAnyException();
    assertThatCode(() -> aspect.authorize(checkout)).doesNotThrowAnyException();
    assertThat(new BillingOwnershipResolver().resolveOwner(billing))
        .isEqualTo(new ActorIdentity(ActorType.CUSTOMER, 42));
  }

  @Test
  void rejectsAnotherCustomerForBothBillingAndCheckout() {
    CurrentActorProvider actorProvider = mock(CurrentActorProvider.class);
    when(actorProvider.getCurrentActor()).thenReturn(new ActorIdentity(ActorType.CUSTOMER, 99));
    Billing billing = billingOwnedBy(42);
    OwnershipAspect aspect =
        new OwnershipAspect(
            actorProvider,
            List.of(new BillingOwnershipResolver(), new BillingCheckoutOwnershipResolver()));

    assertThatThrownBy(() -> aspect.authorize(billing)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> aspect.authorize(checkoutOwnedBy(42, billing)))
        .isInstanceOf(AccessDeniedException.class);
  }

  private Billing billingOwnedBy(long customerId) {
    return new Billing(
        new CustomerId(customerId),
        SubscriptionId.generate(),
        new ProductId(10),
        new Money(30000),
        LocalDate.of(2026, 9, 2),
        LocalDateTime.of(2026, 9, 2, 10, 0));
  }

  private BillingCheckoutDetail checkoutOwnedBy(long customerId, Billing billing) {
    return new BillingCheckoutDetail(
        customerId,
        new BillingCheckoutDetail.SubscriptionInfo("subscription-id", "PAUSED"),
        new BillingCheckoutDetail.ProductInfo(10, "Ethiopia"),
        new BillingCheckoutDetail.BillingInfo(
            billing.getId().value(), 30000, LocalDate.of(2026, 9, 2), "PENDING"));
  }
}
