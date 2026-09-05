package com.bluetoya.beansontime.security.config;

import com.bluetoya.beansontime.security.adapter.springsecurity.AuthenticatedCustomer;
import com.bluetoya.beansontime.security.adapter.springsecurity.AuthenticatedSeller;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    Map<String, UserDetails> users =
        Map.of(
            "customer1",
            new AuthenticatedCustomer(
                1L,
                "customer1",
                passwordEncoder.encode("password1"),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))),
            "customer2",
            new AuthenticatedCustomer(
                2L,
                "customer2",
                passwordEncoder.encode("password2"),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))),
            "seller1",
            new AuthenticatedSeller(
                1L,
                "seller1",
                passwordEncoder.encode("password1"),
                List.of(new SimpleGrantedAuthority("ROLE_SELLER"))),
            "seller2",
            new AuthenticatedSeller(
                2L,
                "seller2",
                passwordEncoder.encode("password2"),
                List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));

    return username -> {
      UserDetails user = users.get(username);

      if (user == null) {
        throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
      }

      return user;
    };
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth -> {
              configureProductAuthorization(auth);
              configureSubscriptionAuthorization(auth);
              configureBillingAuthorization(auth);

              auth.anyRequest().permitAll();
            })
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  private void configureProductAuthorization(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {
    auth.requestMatchers(HttpMethod.GET, "/products", "/products/**")
        .permitAll()
        .requestMatchers(HttpMethod.POST, "/products")
        .hasRole("SELLER");
  }

  private void configureSubscriptionAuthorization(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {
    auth.requestMatchers("/subscriptions", "/subscriptions/**").hasRole("CUSTOMER");
  }

  private void configureBillingAuthorization(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {
    auth.requestMatchers("/billings", "/billings/**").hasRole("CUSTOMER");
  }
}
