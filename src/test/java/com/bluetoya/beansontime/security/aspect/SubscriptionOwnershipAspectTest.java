package com.bluetoya.beansontime.security.aspect;

import static com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture.createSubscription;
import static org.assertj.core.api.Assertions.*;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.security.adapter.springsecurity.AuthenticatedCustomer;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SubscriptionOwnershipAspectTest {

  private SubscriptionOwnershipAspect aspect;

  @BeforeEach
  void setUp() {
    aspect = new SubscriptionOwnershipAspect();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 구독_소유자라면_인가에_성공한다() {
    // given
    CustomerId customerId = new CustomerId(1L);
    Subscription subscription = createSubscription();

    setAuthentication(customerId);

    // when & then
    assertThatCode(() -> aspect.authorizeSubscription(subscription)).doesNotThrowAnyException();
  }

  @Test
  void 구독_소유자가_아니면_인가에_실패한다() {
    // given
    Subscription subscription = createSubscription();

    setAuthentication(new CustomerId(2L));

    // when & then
    assertThatThrownBy(() -> aspect.authorizeSubscription(subscription))
        .isInstanceOf(AccessDeniedException.class);
  }

  private void setAuthentication(CustomerId customerId) {
    AuthenticatedCustomer principal =
        new AuthenticatedCustomer(customerId.id(), "customer", "password", List.of());

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();

    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }
}
