package com.bluetoya.beansontime.product.domain;

import static com.bluetoya.beansontime.product.domain.SupplyStatus.AVAILABLE;
import static com.bluetoya.beansontime.product.domain.SupplyStatus.DISCONTINUED;
import static com.bluetoya.beansontime.product.domain.SupplyStatus.TEMPORARILY_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.product.domain.exception.InvalidSupplyStateChangeException;
import org.junit.jupiter.api.Test;

class ProductTest {

  @Test
  void stopsSupplyFromAvailableState() {
    Product product = product();

    product.stopSupply();

    assertThat(product.getSupplyStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
  }

  @Test
  void repeatedStopSupplyIsIdempotent() {
    Product product = product();
    product.stopSupply();

    product.stopSupply();

    assertThat(product.getSupplyStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
  }

  @Test
  void resumesTemporarilyUnavailableSupply() {
    Product product = product();
    product.stopSupply();

    product.resumeSupply();

    assertThat(product.getSupplyStatus()).isEqualTo(AVAILABLE);
  }

  @Test
  void repeatedResumeSupplyIsIdempotent() {
    Product product = product();

    product.resumeSupply();

    assertThat(product.getSupplyStatus()).isEqualTo(AVAILABLE);
  }

  @Test
  void discontinuesAvailableProduct() {
    Product product = product();

    product.discontinue();

    assertThat(product.getSupplyStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void discontinuesTemporarilyUnavailableProduct() {
    Product product = product();
    product.stopSupply();

    product.discontinue();

    assertThat(product.getSupplyStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void repeatedDiscontinueIsIdempotent() {
    Product product = product();
    product.discontinue();

    product.discontinue();

    assertThat(product.getSupplyStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void discontinuedProductCannotStopSupply() {
    Product product = product();
    product.discontinue();

    assertThatThrownBy(product::stopSupply).isInstanceOf(InvalidSupplyStateChangeException.class);
    assertThat(product.getSupplyStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void discontinuedProductCannotResumeSupply() {
    Product product = product();
    product.discontinue();

    assertThatThrownBy(product::resumeSupply).isInstanceOf(InvalidSupplyStateChangeException.class);
    assertThat(product.getSupplyStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void onlyAvailableProductIsSubscribable() {
    Product available = product();
    Product temporarilyUnavailable = product();
    temporarilyUnavailable.stopSupply();
    Product discontinued = product();
    discontinued.discontinue();

    assertThat(available.isSubscribable()).isTrue();
    assertThat(temporarilyUnavailable.isSubscribable()).isFalse();
    assertThat(discontinued.isSubscribable()).isFalse();
  }

  private Product product() {
    return new Product(new SellerId(1), "Ethiopia", new Money(18000));
  }
}
