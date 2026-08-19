package com.bluetoya.beansontime.security.adapter.springsecurity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityCurrentCustomerProviderTest {

  private SecurityCurrentCustomerProvider provider;

  @BeforeEach
  void setUp() {
    provider = new SecurityCurrentCustomerProvider();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 현재_인증된_고객의_아이디를_반환한다() {
    // given
    AuthenticatedCustomer principal =
        new AuthenticatedCustomer(
            1L, "customer1", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    setAuthentication(principal);

    // when
    CustomerId result = provider.getCurrentCustomerId();

    // then
    assertThat(result).isEqualTo(new CustomerId(1L));
  }

  @Test
  void 인증_정보가_없으면_예외가_발생한다() {
    // when & then
    assertThatThrownBy(() -> provider.getCurrentCustomerId())
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  void 인증된_principal이_고객이_아니면_예외가_발생한다() {
    // given
    Authentication authentication =
        new UsernamePasswordAuthenticationToken("not-customer", null, List.of());

    SecurityContext context = SecurityContextHolder.createEmptyContext();

    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    // when & then
    assertThatThrownBy(() -> provider.getCurrentCustomerId())
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  private void setAuthentication(AuthenticatedCustomer principal) {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();

    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }
}
