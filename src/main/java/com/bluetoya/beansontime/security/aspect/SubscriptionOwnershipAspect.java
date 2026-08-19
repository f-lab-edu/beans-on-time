package com.bluetoya.beansontime.security.aspect;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.security.adapter.springsecurity.AuthenticatedCustomer;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SubscriptionOwnershipAspect {

  private static final String POINTCUT =
      "@annotation(com.bluetoya.beansontime.security.annotation.RequireSubscriptionOwner)";

  @AfterReturning(pointcut = POINTCUT, returning = "subscription")
  public void authorizeSubscription(Subscription subscription) {
    CustomerId currentCustomerId = getCurrentCustomerId();

    if (!subscription.isOwnedBy(currentCustomerId)) {
      throw new AccessDeniedException("Subscription access denied");
    }
  }

  @AfterReturning(pointcut = POINTCUT, returning = "result")
  public void authorizeQueryResult(SubscriptionQueryResult result) {
    CustomerId currentCustomerId = getCurrentCustomerId();
    CustomerId ownerCustomerId = new CustomerId(result.customerId());

    if (!ownerCustomerId.equals(currentCustomerId)) {
      throw new AccessDeniedException("Subscription access denied");
    }
  }

  private CustomerId getCurrentCustomerId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedCustomer customer)) {
      throw new AuthenticationCredentialsNotFoundException("No authenticated customer found");
    }

    return new CustomerId(customer.getCustomerId());
  }
}
