package com.bluetoya.beansontime.payment.adapter.out.gateway.toss;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bluetoya.beansontime.billing.adapter.out.persistence.JdbcBillingAdapter;
import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.adapter.out.persistence.*;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.application.service.*;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    properties = {
      "payment.reconciliation.enabled=false",
      "payment.toss.client-key=test_ck_fixture",
      "payment.toss.secret-key=test_sk_fixture"
    })
@ActiveProfiles("toss-test")
@AutoConfigureMockMvc
@Testcontainers
@Import(TossPaymentIntegrationTest.TimeConfig.class)
class TossPaymentIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  @MockitoBean TossTestPaymentClient client;
  @Autowired JdbcProductAdapter products;
  @Autowired JdbcSubscriptionAdapter subscriptions;
  @Autowired JdbcBillingAdapter billings;
  @Autowired JdbcPaymentAdapter payments;
  @Autowired JdbcPaymentApprovalAdapter approvals;
  @Autowired JdbcPaymentCheckoutAdapter checkouts;
  @Autowired PaymentResultResolver resolver;
  @Autowired PaymentCompletion completion;
  @Autowired PaymentCompletionService normalCompletion;
  @Autowired BillingExecutionPort execution;
  @Autowired CancelGatewayPaymentPort cancellations;
  @Autowired JdbcClient jdbc;
  @Autowired MockMvc mvc;
  @Autowired MutableClock clock;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
  private Billing billing;
  private String order;
  private final String key = "test_payment_key_" + UUID.randomUUID();

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    MutableClock clock() {
      return new MutableClock();
    }
  }

  static class MutableClock extends Clock {
    volatile LocalDateTime now = NOW;

    public ZoneId getZone() {
      return ZoneId.of("Asia/Seoul");
    }

    public Clock withZone(ZoneId zone) {
      return Clock.fixed(instant(), zone);
    }

    public Instant instant() {
      return now.atZone(getZone()).toInstant();
    }
  }

  @BeforeEach
  void setup() throws Exception {
    clock.now = NOW;
    Product product = new Product(new SellerId(1), "원두", new Money(10000));
    products.saveNew(product);
    Subscription subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            LocalDate.of(2026, 9, 1));
    subscription.pause(LocalDate.of(2026, 10, 10), LocalDateTime.of(2026, 9, 30, 10, 0));
    subscriptions.saveNew(subscription);
    billing =
        new Billing(
            new CustomerId(1),
            subscription.getId(),
            product.getId(),
            product.getBasePrice(),
            NOW.toLocalDate(),
            NOW);
    billings.saveNew(billing);
    var response =
        mvc.perform(
                post("/billings/{id}/payment-checkout", billing.getId().value())
                    .with(httpBasic("customer1", "password1")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    order = new JsonMapper().readTree(response).get("orderId").asText();
  }

  private TossTestPaymentClient.PaymentResponse done() {
    return new TossTestPaymentClient.PaymentResponse(
        key,
        order,
        "NORMAL",
        "KRW",
        "DONE",
        10000,
        10000,
        "approval_" + order,
        NOW.atOffset(ZoneOffset.ofHours(9)),
        List.of());
  }

  private TossTestPaymentClient.PaymentResponse cancelled() {
    return new TossTestPaymentClient.PaymentResponse(
        key,
        order,
        "NORMAL",
        "KRW",
        "CANCELED",
        10000,
        0,
        "cancel_" + order,
        NOW.atOffset(ZoneOffset.ofHours(9)),
        List.of(
            new TossTestPaymentClient.CancelResponse(
                "cancel_" + order,
                10000,
                "DONE",
                NOW.plusSeconds(1).atOffset(ZoneOffset.ofHours(9)))));
  }

  private org.springframework.test.web.servlet.ResultActions confirm() throws Exception {
    return mvc.perform(
        post("/billings/{id}/payments", billing.getId().value())
            .with(httpBasic("customer1", "password1"))
            .contentType("application/json")
            .content("{\"orderId\":\"" + order + "\",\"paymentKey\":\"" + key + "\"}"));
  }

  private Payment pending() {
    var payment = Payment.start(billing.getId(), billing.getAmount(), NOW);
    payments.saveAuthorized(payment, new PaymentAuthorization(order, key));
    return payment;
  }

  @Autowired org.springframework.context.ApplicationContext context;

  @Test
  void tossProfileDoesNotRegisterAutomaticRecurringBilling() {
    assertThat(
            context.getBeansOfType(
                com.bluetoya.beansontime.billing.application.port.in.RunRecurringBillingUseCase
                    .class))
        .isEmpty();
    assertThat(
            context.getBeansOfType(
                com.bluetoya.beansontime.billing.application.service.ProcessRecurringBillingService
                    .class))
        .isEmpty();
  }

  @Test
  void confirmsAfterCommittingAttemptAndAuthorizationOutsideTransaction() throws Exception {
    when(client.confirm(any(), anyString()))
        .thenAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              var persisted = payments.findProcessing(billing.getId()).orElseThrow();
              assertThat(checkouts.load(persisted.getId()).orderId()).isEqualTo(order);
              return done();
            });
    confirm().andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PAID);
    assertThat(subscriptions.load(billing.getSubscriptionId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.ACTIVE);
    verify(client, never()).cancel(anyString(), anyString(), anyString());
  }

  @Test
  void recoversLostConfirmationResponseWithoutChargingAgain() throws Exception {
    when(client.confirm(any(), anyString()))
        .thenThrow(new TossTestApiException(0, "UNCONFIRMED_RESPONSE"));
    confirm().andExpect(status().isAccepted());
    var payment = payments.findProcessing(billing.getId()).orElseThrow();
    when(client.find(key)).thenReturn(done());
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("SUCCESS");
    verify(client, times(1)).confirm(any(), anyString());
  }

  @Test
  void rejectsOtherOwnersAndCheckoutBindingBeforeCallingToss() throws Exception {
    mvc.perform(
            post("/billings/{id}/payment-checkout", billing.getId().value())
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());
    String saved = order;
    order = "unrelated_checkout";
    confirm().andExpect(status().isConflict());
    order = saved;
    assertThat(payments.findProcessing(billing.getId())).isEmpty();
    verifyNoInteractions(client);
  }

  @Test
  void cancelsAfterDatabaseRollbackAndClosesBillingAtomically() throws Exception {
    when(client.confirm(any(), anyString())).thenReturn(done());
    when(client.find(key)).thenReturn(done());
    when(client.cancel(eq(key), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              var payment = payments.findProcessing(billing.getId()).orElseThrow();
              assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCEL_PENDING);
              assertThat(approvals.load(payment.getId()).orElseThrow().cancelKey())
                  .isEqualTo(invocation.getArgument(2));
              return cancelled();
            });
    injectSubscriptionFailure();
    try {
      confirm().andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    } finally {
      removeSubscriptionFailure();
    }
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.CANCELLED);
    assertThat(subscriptions.load(billing.getSubscriptionId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.PAUSED);
    var approval =
        jdbc.sql("select * from payment_approvals where transaction_id = :tx")
            .param("tx", done().lastTransactionKey())
            .query()
            .singleRow();
    assertThat(approval.get("phase")).isEqualTo("CANCELLED");
    assertThat(approval.get("transaction_id")).isEqualTo(done().lastTransactionKey());
    mvc.perform(
            post("/subscriptions/{id}/reactivation-billing", billing.getSubscriptionId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk());
    assertThat(billings.findPending(billing.getSubscriptionId()).orElseThrow().getId())
        .isNotEqualTo(billing.getId());
  }

  @Test
  void lostCancellationResponseRemainsBlockedThenRecoversFromQuery() throws Exception {
    when(client.confirm(any(), anyString())).thenReturn(done());
    when(client.find(key)).thenReturn(done());
    when(client.cancel(eq(key), anyString(), anyString()))
        .thenThrow(new TossTestApiException(0, "UNCONFIRMED_RESPONSE"));
    injectSubscriptionFailure();
    try {
      confirm();
    } finally {
      removeSubscriptionFailure();
    }
    var payment = payments.findProcessing(billing.getId()).orElseThrow();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCEL_PENDING);
    confirm().andExpect(status().isConflict());
    clock.now = NOW.plusMinutes(20);
    mvc.perform(
            post("/subscriptions/{id}/reactivation-billing", billing.getSubscriptionId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk());
    assertThat(billings.findPending(billing.getSubscriptionId()).orElseThrow().getId())
        .isEqualTo(billing.getId());
    when(client.find(key)).thenReturn(cancelled());
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("CANCELLED");
    verify(client, times(1)).cancel(anyString(), anyString(), anyString());
  }

  @Test
  void interruptedApplicationWaitsThenConfirmsNonApplicationBeforeCancellation() {
    var payment = pending();
    var approval = PaymentApproval.observed(payment.getId(), done().lastTransactionKey(), NOW);
    approvals.saveNew(approval);
    approvals.save(approval.beginApplication(NOW));
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("PROCESSING");
    verifyNoInteractions(client);
    clock.now = NOW.plusMinutes(2);
    when(client.find(key)).thenReturn(done());
    when(client.cancel(eq(key), anyString(), anyString())).thenReturn(cancelled());
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("CANCELLED");
  }

  @Test
  void alreadyCommittedSuccessNeverCancelsOnRecovery() {
    var payment = pending();
    completion.complete(
        payment.getId(), PaymentGatewayResult.approved(done().lastTransactionKey(), NOW));
    clock.now = NOW.plusHours(1);
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("SUCCESS");
    verifyNoInteractions(client);
  }

  @Test
  void domainConflictPreservesApprovalForReviewWithoutCompensating() {
    var payment = pending();
    var subscription = subscriptions.load(billing.getSubscriptionId()).orElseThrow();
    subscription.cancel();
    subscriptions.save(subscription);
    assertThat(
            completion
                .complete(
                    payment.getId(),
                    PaymentGatewayResult.approved(done().lastTransactionKey(), NOW))
                .getStatus())
        .isEqualTo(PaymentStatus.PROCESSING);
    assertThat(approvals.load(payment.getId()).orElseThrow().phase())
        .isEqualTo(PaymentApproval.Phase.REVIEW);
    clock.now = NOW.plusDays(2);
    resolver.resolve(payment.getId());
    verifyNoInteractions(client);
  }

  @Test
  void lostDatabaseConnectionAfterCommitDoesNotCancelSuccessfulPayment() {
    var payment = pending();
    var uncertain =
        new RecoverablePaymentCompletionService(
            payments,
            payments,
            billings,
            billings,
            subscriptions,
            execution,
            approvals,
            action -> {
              action.run();
              throw new com.bluetoya.beansontime.payment.application.exception
                  .PaymentCommitUncertainException(new IllegalStateException("커밋 응답 유실"));
            },
            cancellations,
            normalCompletion,
            clock);
    assertThat(
            uncertain
                .complete(
                    payment.getId(),
                    PaymentGatewayResult.approved(done().lastTransactionKey(), NOW))
                .getStatus())
        .isEqualTo(PaymentStatus.SUCCESS);
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PAID);
    verifyNoInteractions(client);
  }

  @Test
  void cancellationDecisionWriteFailureRecoversFromDurableApplicationMarker() throws Exception {
    when(client.confirm(any(), anyString())).thenReturn(done());
    injectSubscriptionFailure();
    jdbc.sql(
            "alter table payments add constraint injected_decision_failure check (status <> 'CANCEL_PENDING') not valid")
        .update();
    try {
      assertThatThrownBy(() -> confirm())
          .hasCauseInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    } finally {
      removeSubscriptionFailure();
      jdbc.sql("alter table payments drop constraint injected_decision_failure").update();
    }
    var payment = payments.findProcessing(billing.getId()).orElseThrow();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(approvals.load(payment.getId()).orElseThrow().phase())
        .isEqualTo(PaymentApproval.Phase.APPLYING);
    verify(client, never()).cancel(anyString(), anyString(), anyString());
    clock.now = NOW.plusMinutes(2);
    when(client.find(key)).thenReturn(done());
    when(client.cancel(eq(key), anyString(), anyString())).thenReturn(cancelled());
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("CANCELLED");
  }

  @Test
  void cancellationLocalCommitFailureKeepsAllRecordsPendingUntilQueryRecovery() throws Exception {
    when(client.confirm(any(), anyString())).thenReturn(done());
    when(client.find(key)).thenReturn(done());
    when(client.cancel(eq(key), anyString(), anyString())).thenReturn(cancelled());
    injectSubscriptionFailure();
    jdbc.sql(
            "alter table billings add constraint injected_cancel_failure check (status <> 'CANCELLED') not valid")
        .update();
    try {
      assertThatThrownBy(() -> confirm())
          .hasCauseInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    } finally {
      removeSubscriptionFailure();
      jdbc.sql("alter table billings drop constraint injected_cancel_failure").update();
    }
    var payment = payments.findProcessing(billing.getId()).orElseThrow();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCEL_PENDING);
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PENDING);
    assertThat(approvals.load(payment.getId()).orElseThrow().phase())
        .isEqualTo(PaymentApproval.Phase.CANCEL_PENDING);
    when(client.find(key)).thenReturn(cancelled());
    assertThat(resolver.resolve(payment.getId()).status()).isEqualTo("CANCELLED");
    verify(client, times(1)).cancel(anyString(), anyString(), anyString());
  }

  @Test
  void concurrentApprovalResultsNeverCompensateAnActiveApplication() throws Exception {
    var payment = pending();
    CountDownLatch start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Payment> task =
          () -> {
            start.await(5, TimeUnit.SECONDS);
            return completion.complete(
                payment.getId(), PaymentGatewayResult.approved(done().lastTransactionKey(), NOW));
          };
      var first = pool.submit(task);
      var second = pool.submit(task);
      start.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    }
    assertThat(payments.load(payment.getId()).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.SUCCESS);
    assertThat(subscriptions.load(billing.getSubscriptionId()).orElseThrow().getNextBillingDate())
        .isEqualTo(LocalDate.of(2026, 11, 3));
    verifyNoInteractions(client);
  }

  private void injectSubscriptionFailure() {
    jdbc.sql(
            "alter table subscriptions add constraint toss_injected_failure check (id <> '"
                + billing.getSubscriptionId().value()
                + "'::uuid) not valid")
        .update();
  }

  private void removeSubscriptionFailure() {
    jdbc.sql("alter table subscriptions drop constraint toss_injected_failure").update();
  }
}
