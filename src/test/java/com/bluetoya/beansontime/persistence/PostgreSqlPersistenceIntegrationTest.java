package com.bluetoya.beansontime.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bluetoya.beansontime.billing.adapter.out.persistence.JdbcBillingAdapter;
import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.billing.application.port.out.FindPendingBillingPort;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.adapter.out.gateway.FakePaymentGatewayAdapter;
import com.bluetoya.beansontime.payment.adapter.out.persistence.JdbcPaymentAdapter;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.application.service.PaymentCompletionService;
import com.bluetoya.beansontime.payment.application.service.PaymentResultResolver;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.product.adapter.out.persistence.JdbcProductAdapter;
import com.bluetoya.beansontime.product.application.port.out.ProductExecutionPort;
import com.bluetoya.beansontime.product.domain.*;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.JdbcSubscriptionAdapter;
import com.bluetoya.beansontime.subscription.application.port.out.GetSubscriptionDetailQueryPort;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.*;
import java.util.Optional;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "payment.reconciliation.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
@Import(PostgreSqlPersistenceIntegrationTest.TimeConfig.class)
class PostgreSqlPersistenceIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  @Autowired JdbcProductAdapter products;
  @Autowired JdbcSubscriptionAdapter subscriptions;
  @Autowired GetSubscriptionDetailQueryPort details;
  @Autowired ProductExecutionPort execution;
  @Autowired FindPendingBillingPort billings;
  @Autowired JdbcClient jdbc;
  @Autowired MockMvc mvc;
  @Autowired JdbcBillingAdapter billingStore;
  @Autowired JdbcPaymentAdapter payments;
  @Autowired BillingExecutionPort billingExecution;
  @Autowired PaymentCompletionService completion;
  @Autowired FakePaymentGatewayAdapter gateway;

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(Instant.parse("2026-10-03T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    }
  }

  @Test
  void restoresAllSubscriptionStatesAndIndependentSuspensionReasons() {
    Product product = product();
    Subscription subscription = subscription(product, 1, LocalDate.of(2026, 10, 1));
    assertThat(subscriptions.load(subscription.getId()).orElseThrow())
        .isNotSameAs(subscription)
        .usingRecursiveComparison()
        .isEqualTo(subscription);

    subscription.pause(
        LocalDate.of(2026, 10, 20), LocalDateTime.of(2026, 10, 3, 10, 0, 0, 123456000));
    subscription.addSuspensionReason(SubscriptionSuspensionReason.PAYMENT_FAILED);
    subscription.addSuspensionReason(SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE);
    subscriptions.save(subscription);
    Subscription restored =
        new JdbcSubscriptionAdapter(jdbc).load(subscription.getId()).orElseThrow();
    assertThat(restored).usingRecursiveComparison().isEqualTo(subscription);
    assertThat(details.get(subscription.getId()).subscriptionInfo().currentPeriodStartDate())
        .isNull();
    assertThat(details.get(subscription.getId()).subscriptionInfo().suspensionReasons())
        .containsExactlyInAnyOrderElementsOf(subscription.getSuspensionReasons());

    restored.resume(LocalDate.of(2026, 10, 5));
    subscriptions.save(restored);
    assertThat(subscriptions.load(restored.getId()).orElseThrow())
        .usingRecursiveComparison()
        .isEqualTo(restored);
    restored.cancel();
    subscriptions.save(restored);
    assertThat(subscriptions.load(restored.getId()).orElseThrow())
        .usingRecursiveComparison()
        .isEqualTo(restored);
    assertThat(subscriptions.isExists(restored.getCustomerId(), product.getId())).isFalse();
    assertThat(subscriptions.loadNotCancelled(product.getId())).isEmpty();
    subscription(product, 1, LocalDate.of(2026, 10, 3));
    assertThat(subscriptions.isExists(restored.getCustomerId(), product.getId())).isTrue();
  }

  @Test
  void preservesPauseBusinessDateAtTheLastNanosecondOfTheDay() {
    Product product = product();
    Subscription subscription = subscription(product, 1, LocalDate.of(2026, 10, 1));
    subscription.pause(
        LocalDate.of(2026, 10, 31), LocalDateTime.of(2026, 10, 31, 23, 59, 59, 999999999));
    subscriptions.save(subscription);
    Subscription restored = subscriptions.load(subscription.getId()).orElseThrow();
    assertThat(restored.getPausedAt())
        .isEqualTo(LocalDateTime.of(2026, 10, 31, 23, 59, 59, 999999000));
    assertThat(restored.getRemainingPaidDays()).isZero();
    assertThat(restored.getScheduledResumeDate()).isEqualTo(LocalDate.of(2026, 11, 1));
  }

  @Test
  void rejectsDuplicateIdentityWithoutOverwritingProduct() {
    Product product = product();
    Product collision =
        Product.restore(
            product.getId(), new SellerId(2), "다른 상품", "", new Money(1), SupplyStatus.AVAILABLE);
    assertThatThrownBy(() -> products.saveNew(collision))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(products.load(product.getId()).orElseThrow())
        .usingRecursiveComparison()
        .isEqualTo(product);
  }

  @Test
  void databaseRejectsDuplicateOpenSubscriptionAndInvalidPeriod() {
    Product product = product();
    Subscription subscription = subscription(product, 1, LocalDate.of(2026, 10, 1));
    assertThatThrownBy(() -> subscription(product, 1, LocalDate.of(2026, 10, 2)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.sql("update subscriptions set current_period_end_date = null where id = :id")
                    .param("id", subscription.getId().value())
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(subscriptions.load(subscription.getId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void rollsBackProductAndSubscriptionChangesWhenDatabaseRejectsAnUpdate() throws Exception {
    Product product = product();
    Subscription subscription = subscription(product, 99, LocalDate.of(2026, 10, 1));
    jdbc.sql(
            "alter table subscriptions add constraint injected_failure check (customer_id <> 99) not valid")
        .update();
    try {
      assertThatThrownBy(
              () ->
                  mvc.perform(
                      patch("/products/{id}/supply/stop", product.getId().id())
                          .with(httpBasic("seller1", "password1"))))
          .hasCauseInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbc.sql("alter table subscriptions drop constraint injected_failure").update();
    }
    assertThat(products.load(product.getId()).orElseThrow().getSupplyStatus())
        .isEqualTo(SupplyStatus.AVAILABLE);
    assertThat(subscriptions.load(subscription.getId()).orElseThrow().getSuspensionReasons())
        .isEmpty();
  }

  @Test
  void authorizesPersistedResourcesAndReadsQueriesWithoutChangingThem() throws Exception {
    Product product = product();
    Subscription subscription = subscription(product, 1, LocalDate.of(2026, 10, 1));
    mvc.perform(get("/products/{id}", product.getId().id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("에티오피아"));
    mvc.perform(
            patch("/products/{id}/supply/stop", product.getId().id())
                .with(httpBasic("seller2", "password2")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/subscriptions/{id}", subscription.getId().value())
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/subscriptions/{id}/pause", subscription.getId().value())
                .param("pauseUntilDate", "2026-10-10")
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/subscriptions/{id}", subscription.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subscription.lifecycleStatus").value("ACTIVE"));
    assertThat(subscriptions.load(subscription.getId()).orElseThrow())
        .usingRecursiveComparison()
        .isEqualTo(subscription);
  }

  @Test
  void concurrentSubscriptionRequestsCreateOnlyOneSubscription() throws Exception {
    Product product = product();
    CountDownLatch start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> request =
          () -> {
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return subscribe(product);
          };
      var first = pool.submit(request);
      var second = pool.submit(request);
      start.countDown();
      assertThat(
              java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(subscriptions.loadNotCancelled(product.getId())).hasSize(1);
  }

  @Test
  void supplyChangeAndPausePreserveBothUpdates() throws Exception {
    Product product = product();
    Subscription subscription = subscription(product, 1, LocalDate.of(2026, 10, 1));
    CountDownLatch start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var supply =
          pool.submit(
              () -> {
                start.await(5, TimeUnit.SECONDS);
                return mvc.perform(
                        patch("/products/{id}/supply/stop", product.getId().id())
                            .with(httpBasic("seller1", "password1")))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var pause =
          pool.submit(
              () -> {
                start.await(5, TimeUnit.SECONDS);
                return mvc.perform(
                        patch("/subscriptions/{id}/pause", subscription.getId().value())
                            .param("pauseUntilDate", "2026-10-20")
                            .with(httpBasic("customer1", "password1")))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      start.countDown();
      assertThat(supply.get(10, TimeUnit.SECONDS)).isEqualTo(200);
      assertThat(pause.get(10, TimeUnit.SECONDS)).isEqualTo(200);
    }
    Subscription restored = subscriptions.load(subscription.getId()).orElseThrow();
    assertThat(restored.getLifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    assertThat(restored.getRemainingPaidDays()).isEqualTo(28);
    assertThat(restored.getSuspensionReasons())
        .containsExactly(SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE);
  }

  @Test
  void subscriptionWaitsForSupplyChangeThenChecksLatestProductState() throws Exception {
    Product product = product();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var stop =
          pool.submit(
              () ->
                  execution.execute(
                      product.getId(),
                      () -> {
                        var loaded = products.load(product.getId()).orElseThrow();
                        loaded.stopSupply();
                        products.save(loaded);
                        locked.countDown();
                        try {
                          assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                        } catch (InterruptedException exception) {
                          throw new IllegalStateException(exception);
                        }
                        return null;
                      }));
      try {
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        var request = pool.submit(() -> subscribe(product));
        assertThatThrownBy(() -> request.get(200, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        release.countDown();
        stop.get(10, TimeUnit.SECONDS);
        assertThat(request.get(10, TimeUnit.SECONDS)).isEqualTo(409);
      } finally {
        release.countDown();
      }
    }
    assertThat(subscriptions.loadNotCancelled(product.getId())).isEmpty();
  }

  @Test
  void fakePaymentCanReactivatePersistedSubscriptionAndCheckoutReadsDatabase() throws Exception {
    Product product = product();
    Subscription subscription = subscription(product, 1, LocalDate.of(2026, 9, 1));
    subscription.pause(LocalDate.of(2026, 10, 10), LocalDateTime.of(2026, 9, 30, 10, 0));
    subscriptions.save(subscription);
    mvc.perform(
            post("/subscriptions/{id}/reactivation-billing", subscription.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk());
    var billing = billings.findPending(subscription.getId()).orElseThrow();
    mvc.perform(
            get("/billings/{id}/checkout", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.name").value("에티오피아"));
    mvc.perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
    Subscription restored = subscriptions.load(subscription.getId()).orElseThrow();
    assertThat(restored.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(restored.getCurrentPeriod().startDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    assertThat(restored.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 3));
  }

  @Test
  void persistsPendingAttemptsAndRecoversWithFreshAdaptersUsingApprovalDate() {
    Billing billing = pendingBilling(LocalDateTime.of(2026, 10, 2, 23, 55));
    Payment payment = Payment.start(billing.getId(), billing.getAmount(), billing.getCreatedAt());
    payments.saveNew(payment);
    var restoredPayments = new JdbcPaymentAdapter(jdbc);
    var restoredBillings = new JdbcBillingAdapter(jdbc);
    assertThat(restoredPayments.findProcessingIds()).contains(payment.getId());
    assertThat(restoredPayments.load(payment.getId()).orElseThrow())
        .isNotSameAs(payment)
        .usingRecursiveComparison()
        .isEqualTo(payment);
    var approval =
        PaymentGatewayResult.approved(
            "recovered-" + payment.getId().value(), LocalDateTime.of(2026, 10, 2, 23, 59));
    var restoredCompletion =
        new PaymentCompletionService(
            restoredPayments,
            restoredBillings,
            subscriptions,
            restoredPayments,
            restoredBillings,
            subscriptions,
            billingExecution,
            Clock.fixed(Instant.parse("2026-10-03T01:00:00Z"), ZoneId.of("Asia/Seoul")));
    var resolver =
        new PaymentResultResolver(
            restoredPayments, id -> Optional.of(approval), restoredCompletion);
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("SUCCESS");
    assertThat(restoredBillings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PAID);
    var subscription = subscriptions.load(billing.getSubscriptionId()).orElseThrow();
    assertThat(subscription.getCurrentPeriod().startDate()).isEqualTo(LocalDate.of(2026, 10, 2));
    assertThat(restoredPayments.load(payment.getId()).orElseThrow().getAttemptedAt())
        .isEqualTo(payment.getAttemptedAt());
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("SUCCESS");
    assertThat(subscriptions.load(billing.getSubscriptionId()).orElseThrow())
        .usingRecursiveComparison()
        .isEqualTo(subscription);
  }

  @Test
  void rollsBackAllThreeAggregatesWhenSubscriptionWriteFails() {
    Billing billing = pendingBilling(LocalDateTime.of(2026, 10, 3, 10, 0));
    Payment payment = Payment.start(billing.getId(), billing.getAmount(), billing.getCreatedAt());
    payments.saveNew(payment);
    String constraint = "injected_payment_failure";
    jdbc.sql(
            "alter table subscriptions add constraint "
                + constraint
                + " check (id <> '"
                + billing.getSubscriptionId().value()
                + "'::uuid) not valid")
        .update();
    try {
      assertThatThrownBy(
              () ->
                  completion.complete(
                      payment.getId(),
                      PaymentGatewayResult.approved(
                          "rollback-" + payment.getId().value(), billing.getCreatedAt())))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbc.sql("alter table subscriptions drop constraint " + constraint).update();
    }
    assertThat(payments.load(payment.getId()).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.PROCESSING);
    assertThat(billingStore.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PENDING);
    assertThat(subscriptions.load(billing.getSubscriptionId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.PAUSED);
  }

  @Test
  void concurrentCompletionAppliesOnlyOnceAfterReloadingUnderLock() throws Exception {
    Billing billing = pendingBilling(LocalDateTime.of(2026, 10, 3, 10, 0));
    Payment payment = Payment.start(billing.getId(), billing.getAmount(), billing.getCreatedAt());
    payments.saveNew(payment);
    var result =
        PaymentGatewayResult.approved(
            "concurrent-" + payment.getId().value(), billing.getCreatedAt());
    CountDownLatch start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Payment> task =
          () -> {
            start.await(5, TimeUnit.SECONDS);
            return completion.complete(payment.getId(), result);
          };
      var first = pool.submit(task);
      var second = pool.submit(task);
      start.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
      assertThat(second.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
    assertThat(subscriptions.load(billing.getSubscriptionId()).orElseThrow().getNextBillingDate())
        .isEqualTo(LocalDate.of(2026, 11, 3));
  }

  @Test
  void expiredPaymentRequestCommitsExpirationEvenThoughHttpReturnsConflict() throws Exception {
    Billing billing = pendingBilling(LocalDateTime.of(2026, 10, 3, 9, 50));
    mvc.perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isConflict());
    assertThat(billingStore.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.EXPIRED);
    assertThat(payments.findProcessing(billing.getId())).isEmpty();
  }

  @Test
  void unknownGatewayResponseLeavesDurableAttemptAndBlocksAnotherPayment() throws Exception {
    Billing billing = pendingBilling(LocalDateTime.of(2026, 10, 3, 10, 0));
    gateway.failNext();
    mvc.perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isAccepted());
    var payment = payments.findProcessing(billing.getId()).orElseThrow();
    mvc.perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isConflict());
    gateway.approvePending(payment.getId(), "unknown-" + payment.getId().value());
    mvc.perform(
            post(
                    "/billings/{id}/payments/{paymentId}/reconcile",
                    billing.getId().value(),
                    payment.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
    assertThat(payments.load(payment.getId()).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.SUCCESS);
  }

  @Test
  void constraintsRejectDuplicatePendingBillingsAndProcessingAttempts() {
    Billing billing = pendingBilling(LocalDateTime.of(2026, 10, 3, 10, 0));
    var duplicate =
        new Billing(
            billing.getCustomerId(),
            billing.getSubscriptionId(),
            billing.getProductId(),
            billing.getAmount(),
            billing.getBillingDate(),
            billing.getCreatedAt());
    assertThatThrownBy(() -> billingStore.saveNew(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
    Payment payment = Payment.start(billing.getId(), billing.getAmount(), billing.getCreatedAt());
    payments.saveNew(payment);
    assertThatThrownBy(
            () ->
                payments.saveNew(
                    Payment.start(billing.getId(), billing.getAmount(), billing.getCreatedAt())))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> payments.saveNew(payment))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Billing pendingBilling(LocalDateTime createdAt) {
    Product product = product();
    Subscription subscription = subscription(product, 1, LocalDate.of(2026, 9, 1));
    subscription.pause(LocalDate.of(2026, 10, 10), LocalDateTime.of(2026, 9, 30, 10, 0));
    subscriptions.save(subscription);
    Billing billing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            product.getId(),
            product.getBasePrice(),
            createdAt.toLocalDate(),
            createdAt);
    billingStore.saveNew(billing);
    return billing;
  }

  private int subscribe(Product product) throws Exception {
    return mvc.perform(
            post("/subscriptions")
                .with(httpBasic("customer1", "password1"))
                .contentType("application/json")
                .content(
                    """
          {"productId": %d, "deliveryCycle": {"unit": "ONE_MONTH", "interval": 1}}
          """
                        .formatted(product.getId().id())))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private Product product() {
    Product product = new Product(new SellerId(1), "에티오피아", new Money(18000));
    products.saveNew(product);
    return product;
  }

  private Subscription subscription(Product product, long customerId, LocalDate startedDate) {
    Subscription subscription =
        new Subscription(
            new CustomerId(customerId),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            startedDate);
    subscriptions.saveNew(subscription);
    return subscription;
  }
}
