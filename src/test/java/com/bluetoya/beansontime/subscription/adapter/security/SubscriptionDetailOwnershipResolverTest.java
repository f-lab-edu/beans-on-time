package com.bluetoya.beansontime.subscription.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.product.application.port.in.ProductAvailability;
import com.bluetoya.beansontime.product.application.port.in.ProductInfo;
import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.aspect.OwnershipAspect;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SubscriptionDetailOwnershipResolverTest {

  @Test
  void resolvesCustomerOwnershipAfterSubscriptionInfoExpansion() {
    SubscriptionDetail detail = detailOwnedBy(42);

    ActorIdentity owner = new SubscriptionDetailOwnershipResolver().resolveOwner(detail);

    assertThat(owner).isEqualTo(new ActorIdentity(ActorType.CUSTOMER, 42));
  }

  @Test
  void ownershipAuthorizationAllowsTheSubscriptionOwner() {
    CurrentActorProvider actorProvider = mock(CurrentActorProvider.class);
    when(actorProvider.getCurrentActor()).thenReturn(new ActorIdentity(ActorType.CUSTOMER, 42));
    OwnershipAspect aspect =
        new OwnershipAspect(actorProvider, List.of(new SubscriptionDetailOwnershipResolver()));

    assertThatCode(() -> aspect.authorize(detailOwnedBy(42))).doesNotThrowAnyException();
  }

  @Test
  void ownershipAuthorizationRejectsAnotherCustomer() {
    CurrentActorProvider actorProvider = mock(CurrentActorProvider.class);
    when(actorProvider.getCurrentActor()).thenReturn(new ActorIdentity(ActorType.CUSTOMER, 99));
    OwnershipAspect aspect =
        new OwnershipAspect(actorProvider, List.of(new SubscriptionDetailOwnershipResolver()));

    assertThatThrownBy(() -> aspect.authorize(detailOwnedBy(42)))
        .isInstanceOf(AccessDeniedException.class);
  }

  private SubscriptionDetail detailOwnedBy(long customerId) {
    SubscriptionInfo subscriptionInfo =
        new SubscriptionInfo(
            "subscription-id",
            customerId,
            "ONE_MONTH",
            1,
            "ACTIVE",
            Set.of(),
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 29),
            31,
            LocalDate.of(2026, 9, 30),
            null,
            null,
            false);
    return new SubscriptionDetail(
        subscriptionInfo, new ProductInfo(ProductAvailability.UNAVAILABLE, 10, null, null));
  }
}
