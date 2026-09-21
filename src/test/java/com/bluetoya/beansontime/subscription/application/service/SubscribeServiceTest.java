package com.bluetoya.beansontime.subscription.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.application.exception.ProductNotSubscribableException;
import com.bluetoya.beansontime.subscription.application.port.in.SubscribeCommand;
import com.bluetoya.beansontime.subscription.application.port.out.CurrentCustomerProvider;
import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscribeServiceTest {

  @Test
  void initializesSubscriptionUsingCurrentKstBusinessDate() {
    SaveSubscriptionPort savePort = mock(SaveSubscriptionPort.class);
    ExistsSubscriptionPort existsPort = mock(ExistsSubscriptionPort.class);
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    CurrentCustomerProvider customerProvider = mock(CurrentCustomerProvider.class);
    Clock clock = Clock.fixed(Instant.parse("2026-01-30T15:00:00Z"), ZoneId.of("Asia/Seoul"));
    when(customerProvider.getCurrentCustomerId()).thenReturn(new CustomerId(1));
    when(existsPort.isExists(any(), any())).thenReturn(false);
    Product product = product();
    when(loadProductPort.load(new ProductId(10))).thenReturn(Optional.of(product));
    SubscribeService service =
        new SubscribeService(savePort, existsPort, loadProductPort, customerProvider, clock);

    service.subscribe(
        new SubscribeCommand(new ProductId(10), new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1)));

    ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
    verify(savePort).save(captor.capture());
    Subscription saved = captor.getValue();
    assertThat(saved.getStartedDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(saved.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void rejectsTemporarilyUnavailableProduct() {
    Product product = product();
    product.stopSupply();

    assertProductCannotBeSubscribed(product);
  }

  @Test
  void rejectsDiscontinuedProduct() {
    Product product = product();
    product.discontinue();

    assertProductCannotBeSubscribed(product);
  }

  private void assertProductCannotBeSubscribed(Product product) {
    SaveSubscriptionPort savePort = mock(SaveSubscriptionPort.class);
    ExistsSubscriptionPort existsPort = mock(ExistsSubscriptionPort.class);
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    CurrentCustomerProvider customerProvider = mock(CurrentCustomerProvider.class);
    when(customerProvider.getCurrentCustomerId()).thenReturn(new CustomerId(1));
    when(loadProductPort.load(new ProductId(10))).thenReturn(Optional.of(product));
    SubscribeService service =
        new SubscribeService(
            savePort, existsPort, loadProductPort, customerProvider, Clock.systemUTC());

    assertThatThrownBy(
            () ->
                service.subscribe(
                    new SubscribeCommand(
                        new ProductId(10), new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1))))
        .isInstanceOf(ProductNotSubscribableException.class);

    verify(savePort, never()).save(any());
    verify(existsPort, never()).isExists(any(), any());
  }

  private Product product() {
    return new Product(new SellerId(1), "Ethiopia", new Money(18000));
  }
}
