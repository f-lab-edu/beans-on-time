package com.bluetoya.beansontime.subscription.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.aspect.OwnershipAspect;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SubscriptionOwnershipResolverTest {

  @Test
  void resolvesTheCustomerOwnerFromTheReturnedSubscription() {
    Subscription subscription = subscriptionOwnedBy(42);

    ActorIdentity owner = new SubscriptionOwnershipResolver().resolveOwner(subscription);

    assertThat(owner).isEqualTo(new ActorIdentity(ActorType.CUSTOMER, 42));
  }

  @Test
  void ownershipAspectAuthorizesUsingTheReturnedResourceInsteadOfControllerParameters() {
    CurrentActorProvider actorProvider = mock(CurrentActorProvider.class);
    when(actorProvider.getCurrentActor()).thenReturn(new ActorIdentity(ActorType.CUSTOMER, 42));
    OwnershipAspect aspect =
        new OwnershipAspect(actorProvider, List.of(new SubscriptionOwnershipResolver()));

    assertThatCode(() -> aspect.authorize(subscriptionOwnedBy(42))).doesNotThrowAnyException();
    assertThatThrownBy(() -> aspect.authorize(subscriptionOwnedBy(99)))
        .isInstanceOf(AccessDeniedException.class);
  }

  private Subscription subscriptionOwnedBy(long customerId) {
    return new Subscription(
        new CustomerId(customerId),
        new ProductId(10),
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        LocalDate.of(2026, 9, 1));
  }
}
