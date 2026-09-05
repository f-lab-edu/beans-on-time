package com.bluetoya.beansontime.billing.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.billing.adapter.out.persistence.InMemoryBillingRepository;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductAdapter;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionRepository;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class BillingAuthorizationIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private Clock clock;
  @Autowired private InMemoryProductAdapter productAdapter;
  @Autowired private InMemorySubscriptionRepository subscriptionRepository;
  @Autowired private InMemoryBillingRepository billingRepository;

  @Test
  void independentlyAuthorizesPreparationCheckoutAndPaymentApis() throws Exception {
    LocalDate today = LocalDate.now(clock);
    Product product = new Product(new SellerId(1), "Ethiopia", new Money(30000));
    productAdapter.save(product);
    Subscription subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            today.minusMonths(2).withDayOfMonth(1));
    LocalDate lastPaidDate = subscription.getCurrentPeriod().endDate();
    subscription.pause(lastPaidDate.plusDays(10), lastPaidDate.atTime(10, 0));
    subscriptionRepository.save(subscription);
    Billing billing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            product.getId(),
            product.getBasePrice(),
            today,
            LocalDateTime.now(clock));
    billingRepository.save(billing);

    mockMvc
        .perform(
            post("/subscriptions/{id}/reactivation-billing", subscription.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/subscriptions/{id}/reactivation-billing", subscription.getId().value())
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/billings/{id}/checkout", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/billings/{id}/checkout", billing.getId().value())
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/billings/{id}/checkout", billing.getId().value()))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            get("/billings/{id}/checkout", billing.getId().value())
                .with(httpBasic("seller1", "password1")))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(30000))
        .andExpect(jsonPath("$.status").value("SUCCESS"));
    mockMvc
        .perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isConflict());
  }
}
