package com.bluetoya.beansontime.payment.application.service;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PAYMENT_FAILED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.billing.adapter.out.persistence.InMemoryBillingAdapter;
import com.bluetoya.beansontime.billing.adapter.out.persistence.InMemoryBillingExecutionAdapter;
import com.bluetoya.beansontime.billing.adapter.out.persistence.InMemoryBillingRepository;
import com.bluetoya.beansontime.billing.application.exception.BillingAlreadyPaidException;
import com.bluetoya.beansontime.billing.application.exception.BillingExpiredException;
import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingCommand;
import com.bluetoya.beansontime.billing.application.port.in.PreparedBillingDetail;
import com.bluetoya.beansontime.billing.application.service.PrepareReactivationBillingService;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.billing.domain.BillingStatus;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.adapter.out.gateway.FakePaymentGatewayAdapter;
import com.bluetoya.beansontime.payment.adapter.out.persistence.InMemoryPaymentAdapter;
import com.bluetoya.beansontime.payment.adapter.out.persistence.InMemoryPaymentRepository;
import com.bluetoya.beansontime.payment.application.exception.PaymentInProgressException;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingCommand;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGateway;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.payment.domain.PaymentStatus;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.BillingAnchorDay;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionPeriod;
import com.bluetoya.beansontime.subscription.domain.SubscriptionStatus;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPeriodStateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionResumeDateException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PayBillingServiceTest {
  private final MutableClock clock = new MutableClock();
  private final InMemoryBillingAdapter billings =
      new InMemoryBillingAdapter(new InMemoryBillingRepository());
  private final InMemoryPaymentAdapter payments =
      new InMemoryPaymentAdapter(new InMemoryPaymentRepository());
  private final InMemoryBillingExecutionAdapter execution = new InMemoryBillingExecutionAdapter();
  private final FakePaymentGatewayAdapter gateway = new FakePaymentGatewayAdapter(clock);
  private final OwnedBillingLoader ownedBilling = mock(OwnedBillingLoader.class);
  private final OwnedSubscriptionLoader ownedSubscription = mock(OwnedSubscriptionLoader.class);
  private final LoadSubscriptionPort loadSubscription = mock(LoadSubscriptionPort.class);
  private final SaveSubscriptionPort saveSubscription = mock(SaveSubscriptionPort.class);
  private final LoadProductPort loadProduct = mock(LoadProductPort.class);
  private final Product product = new Product(new SellerId(1), "원두", new Money(30000));
  private Subscription subscription;
  private Billing billing;
  private PayBillingService service;
  private PrepareReactivationBillingService prepare;
  private PaymentCompletionService completion;
  private PaymentResultResolver resolver;

  @BeforeEach
  void setUp() {
    arrangeAt(LocalDateTime.of(2026, 9, 2, 10, 0));
  }

  private void arrangeAt(LocalDateTime now) {
    clock.now = now;
    LocalDate started = now.toLocalDate().minusMonths(2).withDayOfMonth(1);
    subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            started);
    LocalDate pauseDate = subscription.getCurrentPeriod().endDate();
    subscription.pause(pauseDate.plusDays(10), pauseDate.atTime(10, 0));
    billing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            product.getId(),
            new Money(30000),
            now.toLocalDate(),
            now);
    billings.save(billing);
    doAnswer(call -> billings.load(call.getArgument(0)).orElseThrow())
        .when(ownedBilling)
        .load(any());
    when(ownedSubscription.load(subscription.getId())).thenReturn(subscription);
    when(loadSubscription.load(subscription.getId())).thenReturn(Optional.of(subscription));
    when(loadProduct.load(product.getId())).thenReturn(Optional.of(product));
    completion =
        new PaymentCompletionService(
            payments,
            billings,
            loadSubscription,
            payments,
            billings,
            saveSubscription,
            execution,
            clock);
    resolver = new PaymentResultResolver(payments, gateway, completion);
    service = serviceWith(gateway);
    prepare =
        new PrepareReactivationBillingService(
            ownedSubscription, billings, loadProduct, billings, clock, execution, payments);
  }

  private PayBillingService serviceWith(PaymentGateway paymentGateway) {
    return new PayBillingService(
        ownedBilling,
        loadSubscription,
        payments,
        paymentGateway,
        payments,
        billings,
        loadProduct,
        execution,
        completion,
        clock,
        payments);
  }

  private PaymentResult pay() {
    return service.pay(new PayBillingCommand(billing.getId()));
  }

  private PreparedBillingDetail prepare() {
    return prepare.prepare(new PrepareReactivationBillingCommand(subscription.getId()));
  }

  @Test
  void preservesFailedHistoryAndRetriesTheSameSnapshotWithANewPayment() {
    gateway.declineNext();
    PaymentResult first = pay();
    assertThat(first.status()).isEqualTo("FAILED");
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    assertThat(subscription.getRemainingPaidDays()).isZero();
    assertThat(subscription.getSuspensionReasons()).contains(PAYMENT_FAILED);
    when(loadProduct.load(product.getId()))
        .thenReturn(Optional.of(new Product(new SellerId(1), "원두", new Money(35000))));
    assertThat(prepare().billingId()).isEqualTo(billing.getId().value());
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    gateway.approveNext("transaction-1");
    PaymentResult second = pay();
    assertThat(second.paymentId()).isNotEqualTo(first.paymentId());
    assertThat(second.status()).isEqualTo("SUCCESS");
    assertThat(second.amount()).isEqualTo(30000);
    assertThat(payments.load(new PaymentId(first.paymentId())).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.FAILED);
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PAID);
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 10, 1)));
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThatThrownBy(this::pay).isInstanceOf(BillingAlreadyPaidException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {2026, 2028})
  void preservesTheThirtyFirstAnchorAtMonthEnd(int year) {
    arrangeAt(LocalDateTime.of(year, 1, 31, 10, 0));
    pay();
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(31));
    assertThat(subscription.getNextBillingDate())
        .isEqualTo(LocalDate.of(year, 2, year == 2028 ? 29 : 28));
    assertThat(subscription.getCurrentPeriod().endDate())
        .isEqualTo(subscription.getNextBillingDate().minusDays(1));
  }

  @Test
  void keepsUnknownPaymentAndPreventsAnotherAttemptOrReplacementAfterExpiry() {
    gateway.failNext();
    PaymentResult result = pay();
    assertThat(result.status()).isEqualTo("PROCESSING");
    assertThat(subscription.getSuspensionReasons()).doesNotContain(PAYMENT_FAILED);
    clock.now = billing.getExpiresAt();
    assertThat(prepare().billingId()).isEqualTo(billing.getId().value());
    assertThatThrownBy(this::pay).isInstanceOf(PaymentInProgressException.class);
    assertThat(resolver.resolve(new PaymentId(result.paymentId())).status())
        .isEqualTo("PROCESSING");
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
  }

  @Test
  void completesLateApprovalOnceUsingApprovalDateEvenWhenConfirmedTheNextDay() {
    gateway.failNext();
    PaymentResult result = pay();
    PaymentId id = new PaymentId(result.paymentId());
    clock.now = billing.getExpiresAt().plusMinutes(1);
    gateway.approvePending(id, "late-approval");
    clock.now = clock.now.plusDays(1);
    resolver.resolve(id);
    SubscriptionPeriod period = subscription.getCurrentPeriod();
    resolver.resolve(id);
    completion.complete(id, gateway.find(id).orElseThrow());
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PAID);
    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(period.startDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    assertThat(subscription.getCurrentPeriod()).isSameAs(period);
    verify(saveSubscription, times(1)).save(subscription);
  }

  @Test
  void expiresAfterLateDeclineAndPreparesANewBillingAtTheCurrentPrice() {
    gateway.failNext();
    PaymentId id = new PaymentId(pay().paymentId());
    clock.now = billing.getExpiresAt();
    gateway.declinePending(id);
    resolver.resolve(id);
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.EXPIRED);
    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);
    when(loadProduct.load(product.getId()))
        .thenReturn(Optional.of(new Product(new SellerId(1), "원두", new Money(35000))));
    var replacement = prepare();
    assertThat(replacement.billingId()).isNotEqualTo(billing.getId().value());
    assertThat(replacement.amount()).isEqualTo(35000);
    assertThat(replacement.expiresAt()).isEqualTo(clock.now.plusMinutes(10));
    assertThatThrownBy(this::pay).isInstanceOf(BillingExpiredException.class);
    assertThat(service.pay(new PayBillingCommand(new BillingId(replacement.billingId()))).status())
        .isEqualTo("SUCCESS");
  }

  @Test
  void rejectsANewAttemptExactlyAtTheDeadline() {
    clock.now = billing.getExpiresAt();
    assertThatThrownBy(this::pay).isInstanceOf(BillingExpiredException.class);
    assertThat(payments.findProcessingIds()).isEmpty();
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.EXPIRED);
  }

  @Test
  void permitsAnAttemptImmediatelyBeforeTheDeadline() {
    clock.now = billing.getExpiresAt().minusNanos(1);
    assertThat(pay().status()).isEqualTo("SUCCESS");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void blocksUnavailableProductsAtPreparationAndPaymentWithoutRecordingADecline(
      boolean discontinued) {
    if (discontinued) product.discontinue();
    else product.stopSupply();
    PaymentGateway external = mock(PaymentGateway.class);
    service = serviceWith(external);
    assertThatThrownBy(this::prepare).isInstanceOf(ReactivationBillingNotAllowedException.class);
    assertThatThrownBy(this::pay).isInstanceOf(ReactivationBillingNotAllowedException.class);
    assertThat(payments.findProcessingIds()).isEmpty();
    assertThat(subscription.getSuspensionReasons()).doesNotContain(PAYMENT_FAILED);
    verifyNoInteractions(external);
  }

  @Test
  void createsOnlyOnePaymentWhileTheGatewayIsBlocked() throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    PaymentGateway blocking =
        request -> {
          assertThat(payments.findProcessing(request.billingId())).isPresent();
          entered.countDown();
          try {
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
          }
          return PaymentGatewayResult.approved("concurrent", clock.now);
        };
    service = serviceWith(blocking);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(this::pay);
      try {
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        var second =
            executor.submit(
                () -> assertThatThrownBy(this::pay).isInstanceOf(PaymentInProgressException.class));
        second.get(3, TimeUnit.SECONDS);
        assertThat(payments.findProcessingIds()).hasSize(1);
      } finally {
        release.countDown();
      }
      assertThat(first.get(5, TimeUnit.SECONDS).status()).isEqualTo("SUCCESS");
    }
  }

  @Test
  void concurrentPreparationCreatesOneReplacement() throws Exception {
    clock.now = billing.getExpiresAt();
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var tasks = new ArrayList<Future<PreparedBillingDetail>>();
      for (int i = 0; i < 12; i++)
        tasks.add(
            executor.submit(
                () -> {
                  start.await();
                  return prepare();
                }));
      start.countDown();
      Set<Long> ids = new HashSet<>();
      for (var task : tasks) ids.add(task.get(5, TimeUnit.SECONDS).billingId());
      assertThat(ids).hasSize(1).doesNotContain(billing.getId().value());
      assertThat(billing.getStatus()).isEqualTo(BillingStatus.EXPIRED);
    }
  }

  @Test
  void backgroundConfirmationDoesNotResubmitPayment() {
    gateway.failNext();
    PaymentId id = new PaymentId(pay().paymentId());
    gateway.approvePending(id, "background");
    new ReconcileProcessingPaymentsService(payments, resolver).reconcileProcessing();
    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(payments.findProcessingIds()).isEmpty();
  }

  @Test
  void rechecksTheDeadlineAfterLoadingEligibilityData() {
    clock.now = billing.getExpiresAt().minusSeconds(1);
    when(loadProduct.load(product.getId()))
        .thenAnswer(
            call -> {
              clock.now = billing.getExpiresAt();
              return Optional.of(product);
            });
    PaymentGateway external = mock(PaymentGateway.class);
    service = serviceWith(external);
    assertThatThrownBy(this::pay).isInstanceOf(BillingExpiredException.class);
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.EXPIRED);
    assertThat(payments.findProcessingIds()).isEmpty();
    verifyNoInteractions(external);
  }

  @Test
  void recordsLateDeclineAfterCancellationWithoutChangingTheSubscription() {
    gateway.failNext();
    PaymentId id = new PaymentId(pay().paymentId());
    subscription.cancel();
    clock.now = billing.getExpiresAt();
    gateway.declinePending(id);

    assertThat(resolver.resolve(id).status()).isEqualTo("FAILED");

    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(payments.findProcessingIds()).isEmpty();
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.EXPIRED);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    assertThat(subscription.getCurrentPeriod()).isNull();
    assertThat(subscription.getSuspensionReasons()).doesNotContain(PAYMENT_FAILED);
  }

  @Test
  void retainsProcessingAndGatewayEvidenceWhenTheSubscriptionWasCancelledBeforeApproval() {
    gateway.failNext();
    PaymentId id = new PaymentId(pay().paymentId());
    subscription.cancel();
    gateway.approvePending(id, "requires-reconciliation");
    new ReconcileProcessingPaymentsService(payments, resolver).reconcileProcessing();
    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(gateway.find(id).orElseThrow().successful()).isTrue();
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    assertThat(subscription.getCurrentPeriod()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"currentPeriod", "pausedAt", "scheduledResumeDate", "nextBillingDate"})
  void rejectsInvalidPeriodContextBeforeCreatingPaymentOrCallingTheGateway(String fieldName)
      throws Exception {
    // 정상 도메인 행위로 만들 수 없는 잘못된 복원 상태를 재현한다.
    var field = Subscription.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(
        subscription,
        fieldName.equals("currentPeriod")
            ? new SubscriptionPeriod(clock.now.toLocalDate(), clock.now.toLocalDate())
            : null);
    PaymentGateway external = mock(PaymentGateway.class);
    service = serviceWith(external);

    assertThat(subscription.isPaidReactivationTarget()).isTrue();
    assertThatThrownBy(this::pay).isInstanceOf(InvalidSubscriptionPeriodStateException.class);
    assertThat(payments.findProcessingIds()).isEmpty();
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    verifyNoInteractions(external);
  }

  @Test
  void rejectsADateBeforePauseBeforeCreatingPaymentOrCallingTheGateway() throws Exception {
    var pausedAt = Subscription.class.getDeclaredField("pausedAt");
    pausedAt.setAccessible(true);
    pausedAt.set(subscription, clock.now.plusDays(1));
    PaymentGateway external = mock(PaymentGateway.class);
    service = serviceWith(external);

    assertThat(subscription.isPaidReactivationTarget()).isTrue();
    assertThatThrownBy(this::pay).isInstanceOf(InvalidSubscriptionResumeDateException.class);
    assertThat(payments.findProcessingIds()).isEmpty();
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    verifyNoInteractions(external);
  }

  @Test
  void rejectsAMissingSubscriptionBeforeStartingPayment() {
    when(loadSubscription.load(subscription.getId())).thenReturn(Optional.empty());
    PaymentGateway external = mock(PaymentGateway.class);
    service = serviceWith(external);

    assertThatThrownBy(this::pay).isInstanceOf(SubscriptionNotFoundException.class);

    assertThat(payments.findProcessingIds()).isEmpty();
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    verifyNoInteractions(external);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void preservesProcessingWhenTheSubscriptionIsMissingDuringResultApplication(boolean approved) {
    gateway.failNext();
    PaymentId id = new PaymentId(pay().paymentId());
    when(loadSubscription.load(subscription.getId())).thenReturn(Optional.empty());
    if (approved) gateway.approvePending(id, "approval");
    else gateway.declinePending(id);

    assertThatThrownBy(() -> resolver.resolve(id))
        .isInstanceOf(SubscriptionNotFoundException.class);

    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThatThrownBy(this::pay).isInstanceOf(PaymentInProgressException.class);
  }

  private static final class MutableClock extends Clock {
    private volatile LocalDateTime now;

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
}
