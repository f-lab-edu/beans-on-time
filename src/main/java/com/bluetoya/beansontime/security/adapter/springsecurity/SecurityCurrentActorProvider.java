package com.bluetoya.beansontime.security.adapter.springsecurity;

import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentActorProvider implements CurrentActorProvider {
  @Override
  public ActorIdentity getCurrentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      throw new AuthenticationCredentialsNotFoundException("인증 정보를 찾을 수 없습니다.");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof AuthenticatedCustomer customer) {
      return new ActorIdentity(ActorType.CUSTOMER, customer.getId());
    }

    if (principal instanceof AuthenticatedSeller employee) {
      return new ActorIdentity(ActorType.SELLER, employee.getId());
    }

    throw new AuthenticationCredentialsNotFoundException("잘못된 인증 방법입니다.");
  }
}
