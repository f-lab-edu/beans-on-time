package com.bluetoya.beansontime.security.config;

import com.bluetoya.beansontime.security.adapter.springsecurity.AuthenticatedCustomer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    Map<String, AuthenticatedCustomer> customers =
        Map.of(
            "customer1",
            new AuthenticatedCustomer(
                1L,
                "customer1",
                passwordEncoder.encode("password"),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))),
            "customer2",
            new AuthenticatedCustomer(
                2L,
                "customer2",
                passwordEncoder.encode("password2"),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

    return username -> {
      AuthenticatedCustomer customer = customers.get(username);

      if (Objects.isNull(customer)) {
        throw new UsernameNotFoundException("존재하지 않는 고객: " + username);
      }

      return customer;
    };
  }

  @Bean
  DefaultSecurityFilterChain securityFilterChain(HttpSecurity http) {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/subscriptions", "/subscriptions/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .httpBasic(Customizer.withDefaults())
        .build();
  }
}
