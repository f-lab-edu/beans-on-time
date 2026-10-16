package com.bluetoya.beansontime.subscription.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bluetoya.beansontime.billing.application.port.in.RunRecurringBillingUseCase;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.adapter.out.gateway.FakePaymentGatewayAdapter;
import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.service.PaymentResultResolver;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.product.adapter.out.persistence.JdbcProductAdapter;
import com.bluetoya.beansontime.product.domain.*;
import com.bluetoya.beansontime.refund.application.port.in.ReconcileRefundsUseCase;
import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.JdbcSubscriptionAdapter;
import com.bluetoya.beansontime.subscription.application.port.out.WithdrawalStore;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "billing.recurring.enabled=false",
      "payment.reconciliation.enabled=false",
      "refund.reconciliation.enabled=false"
    })
@AutoConfigureMockMvc
@Testcontainers
@Import(SubscriptionWithdrawalIntegrationTest.TimeConfig.class)
class SubscriptionWithdrawalIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  @Autowired JdbcProductAdapter products;
  @Autowired JdbcSubscriptionAdapter subscriptions;
  @Autowired RunRecurringBillingUseCase recurring;
  @Autowired PaymentResultResolver resolver;
  @Autowired ReconcileRefundsUseCase reconcileRefunds;
  @Autowired RefundStore refunds;
  @Autowired WithdrawalStore withdrawals;
  @Autowired JdbcClient jdbc;
  @Autowired MockMvc mvc;
  @Autowired MutableClock clock;
  @MockitoSpyBean FakePaymentGatewayAdapter gateway;
  static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 10, 0);

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
  void resetDatabase() {
    clock.now = NOW;
    jdbc.sql("truncate products cascade").update();
  }

  Subscription subscribe(LocalDate date) {
    var p = new Product(new SellerId(1), "철회 원두", new Money(12000));
    products.saveNew(p);
    var s =
        new Subscription(
            new CustomerId(1), p.getId(), new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1), date);
    subscriptions.saveNew(s);
    return s;
  }

  ResultActions withdraw(Subscription s) throws Exception {
    return mvc.perform(
        post("/subscriptions/{id}/withdrawal", s.getId().value())
            .with(httpBasic("customer1", "password1")));
  }

  long latestId() {
    return jdbc.sql(
            "select p.id from payments p join billings b on b.id=p.billing_id order by b.billing_date desc,p.id desc limit 1")
        .query(Long.class)
        .single();
  }

  @Test
  void refundsAtExactly24HoursAfterCommitAndKeepsFirstDecisionOnRetry() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(1));
    recurring.runDue();
    long id = latestId();
    clock.now = NOW.plusHours(24);
    doAnswer(
            call -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              assertThat(subscriptions.load(s.getId()).orElseThrow().getLifecycleStatus())
                  .isEqualTo(SubscriptionStatus.CANCELLED);
              assertThat(withdrawals.find(s.getId())).isPresent();
              return call.callRealMethod();
            })
        .when(gateway)
        .refundOrFind(any());
    withdraw(s)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.selectedPaymentId").value(id))
        .andExpect(jsonPath("$.decision").value("REFUND_REQUESTED"))
        .andExpect(jsonPath("$.refunds[0].status").value("SUCCEEDED"));
    var first = withdrawals.find(s.getId()).orElseThrow();
    clock.now = NOW.plusDays(5);
    withdraw(s).andExpect(status().isOk()).andExpect(jsonPath("$.selectedPaymentId").value(id));
    assertThat(withdrawals.find(s.getId()).orElseThrow()).isEqualTo(first);
    verify(gateway, times(1)).refundOrFind(any());
    assertThat(subscriptions.load(s.getId()).orElseThrow().getCurrentPeriod()).isNull();
    mvc.perform(
            get("/subscriptions/{id}/withdrawal", s.getId().value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refunds[0].status").value("SUCCEEDED"));
  }

  @Test
  void pastWindowWithdrawsWithoutRefund() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(1));
    recurring.runDue();
    clock.now = NOW.plusHours(24).plusNanos(1000);
    withdraw(s)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("OUTSIDE_WINDOW"))
        .andExpect(jsonPath("$.refunds").isEmpty());
    verify(gateway, never()).refundOrFind(any());
  }

  @Test
  void latestApprovalWinsOverCreationOrderAndNeverFallsBackAfterRefund() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(3));
    recurring.runDue();
    long first = latestId();
    recurring.runDue();
    long second = latestId();
    // 생성 순서와 승인 순서가 다른 이력을 구성한다.
    jdbc.sql("update payments set approved_at=:at where id=:id")
        .param("at", NOW.minusHours(1))
        .param("id", second)
        .update();
    withdraw(s).andExpect(status().isOk()).andExpect(jsonPath("$.selectedPaymentId").value(first));
    withdraw(s).andExpect(status().isOk()).andExpect(jsonPath("$.selectedPaymentId").value(first));
    assertThat(refunds.find(new PaymentId(second))).isEmpty();
  }

  @Test
  void approvalWhileWaitingForWithdrawalLockIsReturnedSeparately() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(3));
    recurring.runDue();
    long first = latestId();
    recurring.runDue();
    long later = latestId();
    // 요청 접수 후 잠금을 얻기 전에 반영된 승인 이력을 구성한다.
    jdbc.sql("update payments set approved_at=:at where id=:id")
        .param("at", NOW.plusSeconds(1))
        .param("id", later)
        .update();
    withdraw(s)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.selectedPaymentId").value(first))
        .andExpect(jsonPath("$.refunds.length()").value(2));
    assertThat(refunds.find(new PaymentId(later)).orElseThrow().isCompleted()).isTrue();
  }

  @Test
  void missingApprovalEvidenceRequiresReviewInsteadOfSelectingKnownOlderPayment() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(3));
    recurring.runDue();
    long old = latestId();
    recurring.runDue();
    jdbc.sql("update payments set approved_at=null where id=:id").param("id", old).update();
    withdraw(s)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("REVIEW_REQUIRED"));
    verify(gateway, never()).refundOrFind(any());
  }

  @Test
  void noPaymentAndPausedSubscriptionCanWithdrawWithoutRefund() throws Exception {
    var s = subscribe(NOW.toLocalDate());
    s.pause(NOW.toLocalDate().plusDays(5), NOW);
    subscriptions.save(s);
    withdraw(s).andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("NO_PAYMENT"));
    assertThat(subscriptions.load(s.getId()).orElseThrow().getRemainingPaidDays()).isNull();
    assertThat(subscriptions.isExists(s.getCustomerId(), s.getProductId())).isFalse();
  }

  @Test
  void forbidsAnotherCustomersWithdrawalAndResultQuery() throws Exception {
    var s = subscribe(NOW.toLocalDate());
    mvc.perform(
            post("/subscriptions/{id}/withdrawal", s.getId().value())
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/subscriptions/{id}/withdrawal", s.getId().value())
                .with(httpBasic("customer2", "password2")))
        .andExpect(status().isForbidden());
    assertThat(subscriptions.load(s.getId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(withdrawals.find(s.getId())).isEmpty();
  }

  @Test
  void lateApprovalIsReturnedSeparatelyWithoutChangingSelectedPayment() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(3));
    recurring.runDue();
    long selected = latestId();
    gateway.failNext();
    recurring.runDue();
    var late = new PaymentId(latestId());
    withdraw(s)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.selectedPaymentId").value(selected));
    clock.now = NOW.plusDays(2);
    gateway.approvePending(late, "late-approval-" + late.value());
    resolver.resolve(late);
    reconcileRefunds.reconcile();
    withdraw(s)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.selectedPaymentId").value(selected))
        .andExpect(jsonPath("$.refunds.length()").value(2));
    assertThat(refunds.find(late).orElseThrow().isCompleted()).isTrue();
    assertThat(withdrawals.find(s.getId()).orElseThrow().requestedAt()).isEqualTo(NOW);
  }

  @Test
  void failedDecisionWriteRollsBackWithdrawalAndRefundBeforeGateway() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(1));
    recurring.runDue();
    jdbc.sql(
            "alter table subscription_withdrawals add constraint injected_failure check(false) not valid")
        .update();
    try {
      assertThatThrownBy(() -> withdraw(s)).hasRootCauseInstanceOf(java.sql.SQLException.class);
    } finally {
      jdbc.sql("alter table subscription_withdrawals drop constraint injected_failure").update();
    }
    assertThat(subscriptions.load(s.getId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(refunds.findBySubscription(s.getId())).isEmpty();
    verify(gateway, never()).refundOrFind(any());
    withdraw(s).andExpect(status().isOk());
  }

  @Test
  void refundOutageDoesNotUndoWithdrawalAndRecoveryUsesSameRequest() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(1));
    recurring.runDue();
    doThrow(new PaymentGatewayUnavailableException("조회 장애")).when(gateway).refundOrFind(any());
    withdraw(s)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refunds[0].status").value("PENDING"));
    var pending = refunds.findBySubscription(s.getId()).getFirst();
    doCallRealMethod().when(gateway).refundOrFind(any());
    clock.now = NOW.plusDays(2);
    reconcileRefunds.reconcile();
    assertThat(refunds.find(pending.paymentId()).orElseThrow().id()).isEqualTo(pending.id());
    assertThat(refunds.find(pending.paymentId()).orElseThrow().isCompleted()).isTrue();
  }

  @Test
  void localRefundCommitFailureRecoversWithoutUndoingWithdrawal() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(1));
    recurring.runDue();
    jdbc.sql(
            "alter table billings add constraint injected_refund_failure check(status<>'REFUNDED') not valid")
        .update();
    try {
      withdraw(s)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.refunds[0].status").value("PENDING"));
    } finally {
      jdbc.sql("alter table billings drop constraint injected_refund_failure").update();
    }
    var refund = refunds.findBySubscription(s.getId()).getFirst();
    assertThat(subscriptions.load(s.getId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.CANCELLED);
    assertThat(jdbc.sql("select status from billings").query(String.class).single())
        .isEqualTo("PAID");
    reconcileRefunds.reconcile();
    assertThat(refunds.find(refund.paymentId()).orElseThrow().id()).isEqualTo(refund.id());
    assertThat(refunds.find(refund.paymentId()).orElseThrow().isCompleted()).isTrue();
    assertThat(jdbc.sql("select status from billings").query(String.class).single())
        .isEqualTo("REFUNDED");
  }

  @Test
  void concurrentWithdrawalRequestsCreateOneDecisionAndRefund() throws Exception {
    var s = subscribe(NOW.toLocalDate().minusMonths(1));
    recurring.runDue();
    var gate = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Void> task =
          () -> {
            gate.await(5, TimeUnit.SECONDS);
            withdraw(s).andExpect(status().isOk());
            return null;
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      gate.countDown();
      a.get(15, TimeUnit.SECONDS);
      b.get(15, TimeUnit.SECONDS);
    }
    assertThat(jdbc.sql("select count(*) from subscription_withdrawals").query(Long.class).single())
        .isEqualTo(1);
    assertThat(refunds.findBySubscription(s.getId())).hasSize(1);
  }
}
