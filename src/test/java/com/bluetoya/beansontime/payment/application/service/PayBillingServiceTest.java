package com.bluetoya.beansontime.payment.application.service;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PAYMENT_FAILED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.billing.application.exception.BillingAlreadyPaidException;
import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingStatus;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.application.exception.PaymentAlreadyAttemptedException;
import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingCommand;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import com.bluetoya.beansontime.payment.application.port.out.ExistsPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGateway;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayRequest;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.application.port.out.SavePaymentPort;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentStatus;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.BillingAnchorDay;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionPeriod;
import com.bluetoya.beansontime.subscription.domain.SubscriptionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PayBillingServiceTest {

  private OwnedBillingLoader ownedBillingLoader;
  private LoadSubscriptionPort loadSubscriptionPort;
  private ExistsPaymentPort existsPaymentPort;
  private PaymentGateway paymentGateway;
  private SavePaymentPort savePaymentPort;
  private SaveBillingPort saveBillingPort;
  private SaveSubscriptionPort saveSubscriptionPort;

  @BeforeEach
  void setUp() {
    ownedBillingLoader = mock(OwnedBillingLoader.class);
    loadSubscriptionPort = mock(LoadSubscriptionPort.class);
    existsPaymentPort = mock(ExistsPaymentPort.class);
    paymentGateway = mock(PaymentGateway.class);
    savePaymentPort = mock(SavePaymentPort.class);
    saveBillingPort = mock(SaveBillingPort.class);
    saveSubscriptionPort = mock(SaveSubscriptionPort.class);
  }

  @Test
  void paysTheBillingAmountAndReactivatesTheSubscriptionOnSuccess() {
    Subscription subscription =
        pausedWithoutRemainingPaidDays(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 9, 1));
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);
    Billing billing = billingFor(subscription);
    arrange(billing, subscription, PaymentGatewayResult.approved("transaction-1"));
    PayBillingService service = serviceAt("2026-09-02T01:00:00Z");

    PaymentResult result = service.pay(new PayBillingCommand(billing.getId()));

    ArgumentCaptor<PaymentGatewayRequest> gatewayRequest =
        ArgumentCaptor.forClass(PaymentGatewayRequest.class);
    verify(paymentGateway).pay(gatewayRequest.capture());
    assertThat(gatewayRequest.getValue().billingId()).isEqualTo(billing.getId());
    assertThat(gatewayRequest.getValue().amount()).isEqualTo(new Money(30000));
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(savePaymentPort).save(paymentCaptor.capture());
    Payment payment = paymentCaptor.getValue();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(payment.getAmount()).isEqualTo(billing.getAmount());
    assertThat(payment.getTransactionId()).isEqualTo("transaction-1");
    assertThat(payment.getAttemptedAt()).isEqualTo(LocalDateTime.of(2026, 9, 2, 10, 0));
    assertThat(result.amount()).isEqualTo(30000);
    assertThat(result.status()).isEqualTo("SUCCESS");
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PAID);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 10, 1)));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(2));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 10, 2));
    assertThat(subscription.getRemainingPaidDays()).isNull();
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getScheduledResumeDate()).isNull();
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    verify(saveBillingPort).save(billing);
    verify(saveSubscriptionPort).save(subscription);
  }

  @Test
  void preservesTheThirtyFirstAnchorAfterAMonthEndPayment() {
    Subscription subscription =
        pausedWithoutRemainingPaidDays(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 30));
    Billing billing = billingFor(subscription);
    arrange(billing, subscription, PaymentGatewayResult.approved("transaction-jan"));

    serviceAt("2026-01-31T01:00:00Z").pay(new PayBillingCommand(billing.getId()));

    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(31));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 27)));
  }

  @Test
  void preservesTheThirtyFirstAnchorUsingLeapYearFebruary() {
    Subscription subscription =
        pausedWithoutRemainingPaidDays(LocalDate.of(2027, 12, 31), LocalDate.of(2028, 1, 30));
    Billing billing = billingFor(subscription);
    arrange(billing, subscription, PaymentGatewayResult.approved("transaction-leap"));

    serviceAt("2028-01-31T01:00:00Z").pay(new PayBillingCommand(billing.getId()));

    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(31));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2028, 2, 29));
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2028, 1, 31), LocalDate.of(2028, 2, 28)));
  }

  @Test
  void recordsADeclinedPaymentWhileKeepingTheBillingAndSubscriptionPaused() {
    Subscription subscription =
        pausedWithoutRemainingPaidDays(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 9, 1));
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    BillingAnchorDay originalAnchor = subscription.getBillingAnchorDay();
    LocalDate originalNextBillingDate = subscription.getNextBillingDate();
    LocalDateTime originalPausedAt = subscription.getPausedAt();
    LocalDate originalScheduledResumeDate = subscription.getScheduledResumeDate();
    Billing billing = billingFor(subscription);
    arrange(billing, subscription, PaymentGatewayResult.declined());

    PaymentResult result =
        serviceAt("2026-09-02T01:00:00Z").pay(new PayBillingCommand(billing.getId()));

    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(savePaymentPort).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(captor.getValue().getTransactionId()).isNull();
    assertThat(result.status()).isEqualTo("FAILED");
    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    assertThat(subscription.getCurrentPeriod()).isNull();
    assertThat(subscription.getRemainingPaidDays()).isZero();
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(originalAnchor);
    assertThat(subscription.getNextBillingDate()).isEqualTo(originalNextBillingDate);
    assertThat(subscription.getPausedAt()).isEqualTo(originalPausedAt);
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(originalScheduledResumeDate);
    assertThat(subscription.getSuspensionReasons())
        .containsExactlyInAnyOrder(PRODUCT_UNAVAILABLE, PAYMENT_FAILED);
    verify(saveBillingPort, never()).save(any());
    verify(saveSubscriptionPort).save(subscription);
  }

  @Test
  void rejectsAnAlreadyPaidBillingBeforeCallingTheGatewayOrCreatingAPayment() {
    Subscription subscription =
        pausedWithoutRemainingPaidDays(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 9, 1));
    Billing billing = billingFor(subscription);
    billing.markPaid();
    when(ownedBillingLoader.load(billing.getId())).thenReturn(billing);

    assertThatThrownBy(
            () -> serviceAt("2026-09-02T01:00:00Z").pay(new PayBillingCommand(billing.getId())))
        .isInstanceOf(BillingAlreadyPaidException.class);

    verify(loadSubscriptionPort, never()).load(any());
    verify(paymentGateway, never()).pay(any());
    verify(savePaymentPort, never()).save(any());
  }

  @Test
  void rejectsASecondAttemptForAPendingBillingAfterAPreviousFailure() {
    Subscription subscription =
        pausedWithoutRemainingPaidDays(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 9, 1));
    Billing billing = billingFor(subscription);
    when(ownedBillingLoader.load(billing.getId())).thenReturn(billing);
    when(existsPaymentPort.existsByBillingId(billing.getId())).thenReturn(true);

    assertThatThrownBy(
            () -> serviceAt("2026-09-02T01:00:00Z").pay(new PayBillingCommand(billing.getId())))
        .isInstanceOf(PaymentAlreadyAttemptedException.class);

    verify(loadSubscriptionPort, never()).load(any());
    verify(paymentGateway, never()).pay(any());
    verify(savePaymentPort, never()).save(any());
  }

  @Test
  void treatsGatewayUnavailabilityAsAnExternalFailureWithoutRecordingADecline() {
    Subscription subscription =
        pausedWithoutRemainingPaidDays(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 9, 1));
    Billing billing = billingFor(subscription);
    when(ownedBillingLoader.load(billing.getId())).thenReturn(billing);
    when(loadSubscriptionPort.load(subscription.getId())).thenReturn(Optional.of(subscription));
    when(paymentGateway.pay(any()))
        .thenThrow(new PaymentGatewayUnavailableException("gateway timeout"));

    assertThatThrownBy(
            () -> serviceAt("2026-09-02T01:00:00Z").pay(new PayBillingCommand(billing.getId())))
        .isInstanceOf(PaymentGatewayUnavailableException.class);

    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    assertThat(subscription.getSuspensionReasons()).doesNotContain(PAYMENT_FAILED);
    verify(savePaymentPort, never()).save(any());
    verify(saveBillingPort, never()).save(any());
    verify(saveSubscriptionPort, never()).save(any());
  }

  private void arrange(
      Billing billing, Subscription subscription, PaymentGatewayResult gatewayResult) {
    when(ownedBillingLoader.load(billing.getId())).thenReturn(billing);
    when(loadSubscriptionPort.load(subscription.getId())).thenReturn(Optional.of(subscription));
    when(paymentGateway.pay(any())).thenReturn(gatewayResult);
  }

  private PayBillingService serviceAt(String instant) {
    Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul"));
    return new PayBillingService(
        ownedBillingLoader,
        loadSubscriptionPort,
        existsPaymentPort,
        paymentGateway,
        savePaymentPort,
        saveBillingPort,
        saveSubscriptionPort,
        clock);
  }

  private Billing billingFor(Subscription subscription) {
    return new Billing(
        subscription.getCustomerId(),
        subscription.getId(),
        subscription.getProductId(),
        new Money(30000),
        subscription.getPausedAt().toLocalDate(),
        subscription.getPausedAt());
  }

  private Subscription pausedWithoutRemainingPaidDays(LocalDate startedDate, LocalDate pauseDate) {
    Subscription subscription =
        new Subscription(
            new CustomerId(1),
            new ProductId(10),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            startedDate);
    subscription.pause(pauseDate.plusDays(10), pauseDate.atTime(10, 0));
    assertThat(subscription.getRemainingPaidDays()).isZero();
    return subscription;
  }
}
