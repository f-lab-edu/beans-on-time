package com.bluetoya.beansontime.billing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingCommand;
import com.bluetoya.beansontime.billing.application.port.in.PreparedBillingDetail;
import com.bluetoya.beansontime.billing.application.port.out.FindPendingBillingPort;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingStatus;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.product.domain.SellerId;
import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PrepareReactivationBillingServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-02T01:00:00Z"), ZoneId.of("Asia/Seoul"));

  private OwnedSubscriptionLoader ownedSubscriptionLoader;
  private FindPendingBillingPort findPendingBillingPort;
  private LoadProductPort loadProductPort;
  private SaveBillingPort saveBillingPort;
  private PrepareReactivationBillingService service;

  @BeforeEach
  void setUp() {
    ownedSubscriptionLoader = mock(OwnedSubscriptionLoader.class);
    findPendingBillingPort = mock(FindPendingBillingPort.class);
    loadProductPort = mock(LoadProductPort.class);
    saveBillingPort = mock(SaveBillingPort.class);
    service =
        new PrepareReactivationBillingService(
            ownedSubscriptionLoader,
            findPendingBillingPort,
            loadProductPort,
            saveBillingPort,
            CLOCK);
  }

  @Test
  void preparesAPendingBillingUsingTheCurrentProductPriceAndKstTime() {
    Subscription subscription = pausedWithoutRemainingPaidDays();
    Product product = new Product(new SellerId(1), "Ethiopia", new Money(30000));
    when(ownedSubscriptionLoader.load(subscription.getId())).thenReturn(subscription);
    when(findPendingBillingPort.findPending(subscription.getId())).thenReturn(Optional.empty());
    when(loadProductPort.load(subscription.getProductId())).thenReturn(Optional.of(product));

    PreparedBillingDetail detail =
        service.prepare(new PrepareReactivationBillingCommand(subscription.getId()));

    ArgumentCaptor<Billing> captor = ArgumentCaptor.forClass(Billing.class);
    verify(saveBillingPort).save(captor.capture());
    Billing saved = captor.getValue();
    assertThat(saved.getCustomerId()).isEqualTo(subscription.getCustomerId());
    assertThat(saved.getSubscriptionId()).isEqualTo(subscription.getId());
    assertThat(saved.getProductId()).isEqualTo(subscription.getProductId());
    assertThat(saved.getAmount()).isEqualTo(new Money(30000));
    assertThat(saved.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(saved.getBillingDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 2, 10, 0));
    assertThat(detail.billingId()).isEqualTo(saved.getId().value());
    assertThat(detail.amount()).isEqualTo(30000);
  }

  @Test
  void returnsTheExistingPendingBillingWithoutReadingTheChangedProductPrice() {
    Subscription subscription = pausedWithoutRemainingPaidDays();
    Billing existing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            subscription.getProductId(),
            new Money(30000),
            LocalDate.of(2026, 9, 1),
            LocalDateTime.of(2026, 9, 1, 10, 0));
    when(ownedSubscriptionLoader.load(subscription.getId())).thenReturn(subscription);
    when(findPendingBillingPort.findPending(subscription.getId()))
        .thenReturn(Optional.of(existing));

    PreparedBillingDetail detail =
        service.prepare(new PrepareReactivationBillingCommand(subscription.getId()));

    assertThat(detail.billingId()).isEqualTo(existing.getId().value());
    assertThat(detail.amount()).isEqualTo(30000);
    verify(loadProductPort, never()).load(subscription.getProductId());
    verify(saveBillingPort, never()).save(existing);
  }

  @Test
  void rejectsAnActiveSubscription() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 2));
    when(ownedSubscriptionLoader.load(subscription.getId())).thenReturn(subscription);

    assertNotAllowed(subscription);
  }

  @Test
  void rejectsAPausedSubscriptionWithRemainingPaidDays() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 2));
    subscription.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    when(ownedSubscriptionLoader.load(subscription.getId())).thenReturn(subscription);

    assertNotAllowed(subscription);
  }

  @Test
  void rejectsACancelledSubscription() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 2));
    subscription.cancel();
    when(ownedSubscriptionLoader.load(subscription.getId())).thenReturn(subscription);

    assertNotAllowed(subscription);
  }

  @Test
  void rejectsPreparationWhenTheReferencedProductDoesNotExist() {
    Subscription subscription = pausedWithoutRemainingPaidDays();
    when(ownedSubscriptionLoader.load(subscription.getId())).thenReturn(subscription);
    when(findPendingBillingPort.findPending(subscription.getId())).thenReturn(Optional.empty());
    when(loadProductPort.load(subscription.getProductId())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.prepare(new PrepareReactivationBillingCommand(subscription.getId())))
        .isInstanceOf(ProductNotFoundException.class);

    verify(saveBillingPort, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private void assertNotAllowed(Subscription subscription) {
    assertThatThrownBy(
            () -> service.prepare(new PrepareReactivationBillingCommand(subscription.getId())))
        .isInstanceOf(ReactivationBillingNotAllowedException.class);
    verify(findPendingBillingPort, never()).findPending(subscription.getId());
    verify(saveBillingPort, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private Subscription pausedWithoutRemainingPaidDays() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 8, 2));
    subscription.pause(LocalDate.of(2026, 9, 10), LocalDateTime.of(2026, 9, 1, 10, 0));
    assertThat(subscription.getRemainingPaidDays()).isZero();
    return subscription;
  }

  private Subscription subscriptionStartedOn(LocalDate startedDate) {
    return new Subscription(
        new CustomerId(1),
        new ProductId(10),
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        startedDate);
  }
}
