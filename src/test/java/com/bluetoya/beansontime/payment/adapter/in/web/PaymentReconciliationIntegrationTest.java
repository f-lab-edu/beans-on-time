package com.bluetoya.beansontime.payment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.billing.adapter.out.persistence.InMemoryBillingRepository;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.adapter.out.gateway.FakePaymentGatewayAdapter;
import com.bluetoya.beansontime.payment.adapter.out.persistence.InMemoryPaymentRepository;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductAdapter;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionRepository;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
      "payment.reconciliation.enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("in-memory")
class PaymentReconciliationIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private Clock clock;
  @Autowired private InMemoryProductAdapter products;
  @Autowired private InMemorySubscriptionRepository subscriptions;
  @Autowired private InMemoryBillingRepository billings;
  @Autowired private InMemoryPaymentRepository payments;
  @Autowired private FakePaymentGatewayAdapter gateway;
  private Billing billing;
  private Payment payment;
  private Subscription subscription;

  @BeforeEach
  void setUp() throws Exception {
    var product = new Product(new SellerId(1), "원두", new Money(30000));
    products.save(product);
    subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            LocalDate.now(clock).minusMonths(2).withDayOfMonth(1));
    var lastDate = subscription.getCurrentPeriod().endDate();
    subscription.pause(lastDate.plusDays(10), lastDate.atTime(10, 0));
    subscriptions.save(subscription);
    billing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            product.getId(),
            product.getBasePrice(),
            LocalDate.now(clock),
            LocalDateTime.now(clock));
    billings.save(billing);
    gateway.failNext();
    mockMvc
        .perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("PROCESSING"));
    payment = payments.findProcessing(billing.getId()).orElseThrow();
  }

  @Test
  void authorizesResultConfirmationAndAppliesApprovalOnlyOnce() throws Exception {
    String url =
        "/billings/"
            + billing.getId().value()
            + "/payments/"
            + payment.getId().value()
            + "/reconcile";
    mockMvc.perform(post(url)).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post(url).with(httpBasic("seller1", "password1")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post(url).with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post(url).with(httpBasic("customer1", "password1")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("PROCESSING"));
    mockMvc
        .perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.paymentId").value(payment.getId().value()));
    gateway.approvePending(payment.getId(), "confirmed");
    mockMvc
        .perform(post(url).with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
    var period = subscription.getCurrentPeriod();
    mockMvc
        .perform(post(url).with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").value("confirmed"));
    assertThat(subscription.getCurrentPeriod()).isSameAs(period);
  }

  @Test
  void rejectsAPaymentThatDoesNotBelongToTheAuthorizedBilling() throws Exception {
    Billing other =
        new Billing(
            billing.getCustomerId(),
            billing.getSubscriptionId(),
            billing.getProductId(),
            billing.getAmount(),
            LocalDate.now(clock),
            LocalDateTime.now(clock));
    billings.save(other);
    mockMvc
        .perform(
            post(
                    "/billings/{billing}/payments/{payment}/reconcile",
                    other.getId().value(),
                    payment.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/billings/{billing}/payments/0/reconcile", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isBadRequest());
  }
}
