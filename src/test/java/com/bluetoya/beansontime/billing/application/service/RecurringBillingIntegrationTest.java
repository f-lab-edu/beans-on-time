package com.bluetoya.beansontime.billing.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.billing.adapter.out.persistence.JdbcBillingAdapter;
import com.bluetoya.beansontime.billing.application.port.in.*;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.adapter.out.gateway.FakePaymentGatewayAdapter;
import com.bluetoya.beansontime.payment.adapter.out.persistence.JdbcPaymentAdapter;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayRequest;
import com.bluetoya.beansontime.payment.application.service.PaymentResultResolver;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.product.adapter.out.persistence.JdbcProductAdapter;
import com.bluetoya.beansontime.product.domain.*;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.JdbcSubscriptionAdapter;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {"payment.reconciliation.enabled=false", "billing.recurring.enabled=false"})
@AutoConfigureMockMvc
@Testcontainers
@Import(RecurringBillingIntegrationTest.TimeConfig.class)
class RecurringBillingIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  @Autowired RunRecurringBillingUseCase run;
  @Autowired ProcessRecurringBillingService process;
  @Autowired JdbcProductAdapter products;
  @Autowired JdbcSubscriptionAdapter subscriptions;
  @Autowired JdbcBillingAdapter billings;
  @Autowired JdbcPaymentAdapter payments;
  @Autowired PaymentResultResolver resolver;
  @Autowired JdbcClient jdbc;
  @Autowired MockMvc mvc;
  @MockitoSpyBean FakePaymentGatewayAdapter gateway;
  private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-10-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    }
  }

  @BeforeEach
  void resetDatabase() {
    jdbc.sql("truncate products cascade").update();
  }

  private Subscription subscribe(LocalDate started) {
    var product = new Product(new SellerId(1), "정기 원두", new Money(12000));
    products.saveNew(product);
    var subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            started);
    subscriptions.saveNew(subscription);
    return subscription;
  }

  private Billing billing(Subscription subscription) {
    return billings
        .findRecurring(subscription.getId(), subscription.getNextBillingDate())
        .orElseThrow();
  }

  private Payment payment(Billing billing) {
    return payments
        .load(
            new PaymentId(
                jdbc.sql("select id from payments where billing_id = :id")
                    .param("id", billing.getId().value())
                    .query(Long.class)
                    .single()))
        .orElseThrow();
  }

  @Test
  void commitsBillingAndAttemptBeforeCallingGatewayThenRenewsOnlyOnce() {
    var subscription = subscribe(DATE.minusMonths(1));
    doAnswer(
            call -> {
              var request = (PaymentGatewayRequest) call.getArgument(0);
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              assertThat(payments.load(request.paymentId()).orElseThrow().getStatus())
                  .isEqualTo(PaymentStatus.PROCESSING);
              assertThat(billings.load(request.billingId()).orElseThrow().getPurpose())
                  .isEqualTo(BillingPurpose.RECURRING);
              return call.callRealMethod();
            })
        .when(gateway)
        .pay(any());
    run.runDue();
    var billing = billing(subscription);
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PAID);
    assertThat(billing.getExpiresAt()).isNull();
    assertThat(payment(billing).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    var renewed = subscriptions.load(subscription.getId()).orElseThrow();
    assertThat(renewed.getCurrentPeriod().startDate()).isEqualTo(DATE);
    assertThat(renewed.getNextBillingDate()).isEqualTo(DATE.plusMonths(1));
    run.runDue();
    process.process(subscription.getId(), DATE, DATE);
    verify(gateway, times(1)).pay(any());
  }

  @Test
  void resumesAnOlderPreparedBillingWithOriginalPriceAndDueDate() {
    var subscription = subscribe(LocalDate.of(2026, 9, 3));
    var existing =
        Billing.recurring(
            subscription.getCustomerId(),
            subscription.getId(),
            subscription.getProductId(),
            new Money(9000),
            subscription.getNextBillingDate(),
            LocalDateTime.of(2026, 10, 3, 0, 0));
    billings.saveNew(existing);
    run.runDue();
    var saved = billing(subscription);
    assertThat(saved.getId()).isEqualTo(existing.getId());
    assertThat(payment(saved).getAmount()).isEqualTo(new Money(9000));
    var renewed = subscriptions.load(subscription.getId()).orElseThrow();
    assertThat(renewed.getCurrentPeriod().startDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    assertThat(renewed.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 3));
  }

  @Test
  void pendingResultIsQueriedWithoutStartingAnotherAttempt() {
    var subscription = subscribe(DATE.minusMonths(1));
    gateway.failNext();
    run.runDue();
    var billing = billing(subscription);
    var payment = payment(billing);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    run.runDue();
    gateway.approvePending(payment.getId(), "recurring-late-approval");
    resolver.resolve(payment.getId());
    resolver.resolve(payment.getId());
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PAID);
    assertThat(subscriptions.load(subscription.getId()).orElseThrow().getNextBillingDate())
        .isEqualTo(DATE.plusMonths(1));
    verify(gateway, times(1)).pay(any());
  }

  @Test
  void declineBlocksExecutionWithoutAutomaticallyRetryingOrPausing() {
    var subscription = subscribe(DATE.minusMonths(1));
    gateway.declineNext();
    run.runDue();
    var billing = billing(subscription);
    assertThat(payment(billing).getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    var current = subscriptions.load(subscription.getId()).orElseThrow();
    assertThat(current.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(current.getSuspensionReasons())
        .containsExactly(SubscriptionSuspensionReason.PAYMENT_FAILED);
    assertThat(current.getNextBillingDate()).isEqualTo(DATE);
    run.runDue();
    // 차단 사유만 해제해도 기존 실패 시도를 자동으로 재결제하지 않는다.
    current.removeSuspensionReason(SubscriptionSuspensionReason.PAYMENT_FAILED);
    subscriptions.save(current);
    run.runDue();
    verify(gateway, times(1)).pay(any());
  }

  @Test
  void rechecksEligibilityAfterCandidateWasReadAndExcludesFutureOrBlockedSubscriptions() {
    var paused = subscribe(DATE.minusMonths(1));
    paused.pause(DATE.plusDays(3), DATE.minusDays(1).atStartOfDay());
    subscriptions.save(paused);
    var unavailable = subscribe(DATE.minusMonths(1));
    var product = products.load(unavailable.getProductId()).orElseThrow();
    product.stopSupply();
    products.save(product);
    var blocked = subscribe(DATE.minusMonths(1));
    blocked.addSuspensionReason(SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE);
    subscriptions.save(blocked);
    var future = subscribe(DATE);
    var cancelled = subscribe(DATE.minusMonths(1));
    cancelled.cancel();
    subscriptions.save(cancelled);
    run.runDue();
    process.process(paused.getId(), DATE, DATE);
    process.process(cancelled.getId(), DATE, DATE);
    process.process(future.getId(), DATE, DATE);
    assertThat(jdbc.sql("select count(*) from billings").query(Long.class).single()).isZero();
    verify(gateway, never()).pay(any());
  }

  @Test
  void simultaneousExecutionsCreateOneBillingAndOneAttempt() throws Exception {
    var subscription = subscribe(DATE.minusMonths(1));
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Void> task =
          () -> {
            start.await(5, TimeUnit.SECONDS);
            process.process(subscription.getId(), DATE, DATE);
            return null;
          };
      var first = pool.submit(task);
      var second = pool.submit(task);
      start.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    }
    assertThat(jdbc.sql("select count(*) from billings").query(Long.class).single()).isEqualTo(1);
    assertThat(jdbc.sql("select count(*) from payments").query(Long.class).single()).isEqualTo(1);
    verify(gateway, times(1)).pay(any());
  }

  @Test
  void databaseRejectsDuplicateDueDateEvenAfterPaymentCompletes() {
    var subscription = subscribe(DATE.minusMonths(1));
    run.runDue();
    var duplicate =
        Billing.recurring(
            subscription.getCustomerId(),
            subscription.getId(),
            subscription.getProductId(),
            new Money(12000),
            DATE,
            DATE.atStartOfDay());
    assertThatThrownBy(() -> billings.saveNew(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void paymentInsertionFailureRollsBackNewBillingAndNextRunCanStart() {
    var subscription = subscribe(DATE.minusMonths(1));
    jdbc.sql("alter table payments add constraint injected_start_failure check (false) not valid")
        .update();
    try {
      assertThatThrownBy(() -> process.process(subscription.getId(), DATE, DATE))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbc.sql("alter table payments drop constraint injected_start_failure").update();
    }
    assertThat(billings.findRecurring(subscription.getId(), DATE)).isEmpty();
    verify(gateway, never()).pay(any());
    run.runDue();
    assertThat(billing(subscription).getStatus()).isEqualTo(BillingStatus.PAID);
  }

  @Test
  void failedLocalCompletionKeepsProcessingAndRecoversWithoutAnotherCharge() {
    var subscription = subscribe(DATE.minusMonths(1));
    jdbc.sql(
            "alter table subscriptions add constraint injected_renewal_failure check (next_billing_date <= date '2026-10-04') not valid")
        .update();
    try {
      run.runDue();
    } finally {
      jdbc.sql("alter table subscriptions drop constraint injected_renewal_failure").update();
    }
    var billing = billing(subscription);
    var payment = payment(billing);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(subscriptions.load(subscription.getId()).orElseThrow().getNextBillingDate())
        .isEqualTo(DATE);
    resolver.resolve(payment.getId());
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PAID);
    verify(gateway, times(1)).pay(any());
  }

  @Test
  void processesMultiplePagesAndContinuesPastOneFailure() {
    var candidates = new ArrayList<Subscription>();
    for (int i = 0; i < 101; i++) candidates.add(subscribe(DATE.minusMonths(1)));
    candidates.sort(Comparator.comparing(s -> s.getId().value().toString()));
    var failed = candidates.getFirst();
    jdbc.sql(
            "alter table subscriptions add constraint injected_one_failure check (id <> '"
                + failed.getId().value()
                + "'::uuid) not valid")
        .update();
    try {
      run.runDue();
    } finally {
      jdbc.sql("alter table subscriptions drop constraint injected_one_failure").update();
    }
    assertThat(
            jdbc.sql("select count(*) from billings where status = 'PAID'")
                .query(Long.class)
                .single())
        .isEqualTo(100);
    assertThat(payment(billing(failed)).getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(jdbc.sql("select count(*) from payments").query(Long.class).single()).isEqualTo(101);
  }

  @Test
  void longOverdueSubscriptionAdvancesOnlyOnePeriodPerRun() {
    var subscription = subscribe(LocalDate.of(2026, 7, 4));
    run.runDue();
    assertThat(subscriptions.load(subscription.getId()).orElseThrow().getNextBillingDate())
        .isEqualTo(LocalDate.of(2026, 9, 4));
    verify(gateway, times(1)).pay(any());
  }

  @Test
  void customerCannotUseReactivationApiToPayRecurringBilling() throws Exception {
    var subscription = subscribe(DATE.minusMonths(1));
    var billing =
        Billing.recurring(
            subscription.getCustomerId(),
            subscription.getId(),
            subscription.getProductId(),
            new Money(12000),
            DATE,
            DATE.atStartOfDay());
    billings.saveNew(billing);
    mvc.perform(
            post("/billings/{id}/payments", billing.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isConflict());
    verify(gateway, never()).pay(any());
  }
}
