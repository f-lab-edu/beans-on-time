package com.bluetoya.beansontime.security.adapter.springsecurity;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.security.application.CurrentCustomerProvider;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentCustomerProvider implements CurrentCustomerProvider {
  @Override
  public CustomerId getCurrentCustomerId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedCustomer customer)) {
      throw new AuthenticationCredentialsNotFoundException("No authenticated customer found");
    }

    return new CustomerId(customer.getCustomerId());
  }
}
