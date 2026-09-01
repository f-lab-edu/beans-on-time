package com.bluetoya.beansontime.product.domain;

import static com.bluetoya.beansontime.product.domain.ProductStatus.AVAILABLE;
import static com.bluetoya.beansontime.product.domain.ProductStatus.DISCONTINUED;
import static com.bluetoya.beansontime.product.domain.ProductStatus.TEMPORARILY_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.product.domain.exception.InvalidProductStateChangeException;
import org.junit.jupiter.api.Test;

class ProductTest {

  @Test
  void stopsSupplyFromAvailableState() {
    Product product = product();

    product.stopSupply();

    assertThat(product.getStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
  }

  @Test
  void repeatedStopSupplyIsIdempotent() {
    Product product = product();
    product.stopSupply();

    product.stopSupply();

    assertThat(product.getStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
  }

  @Test
  void resumesTemporarilyUnavailableSupply() {
    Product product = product();
    product.stopSupply();

    product.resumeSupply();

    assertThat(product.getStatus()).isEqualTo(AVAILABLE);
  }

  @Test
  void repeatedResumeSupplyIsIdempotent() {
    Product product = product();

    product.resumeSupply();

    assertThat(product.getStatus()).isEqualTo(AVAILABLE);
  }

  @Test
  void discontinuesAvailableProduct() {
    Product product = product();

    product.discontinue();

    assertThat(product.getStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void discontinuesTemporarilyUnavailableProduct() {
    Product product = product();
    product.stopSupply();

    product.discontinue();

    assertThat(product.getStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void repeatedDiscontinueIsIdempotent() {
    Product product = product();
    product.discontinue();

    product.discontinue();

    assertThat(product.getStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void discontinuedProductCannotStopSupply() {
    Product product = product();
    product.discontinue();

    assertThatThrownBy(product::stopSupply).isInstanceOf(InvalidProductStateChangeException.class);
    assertThat(product.getStatus()).isEqualTo(DISCONTINUED);
  }

  @Test
  void discontinuedProductCannotResumeSupply() {
    Product product = product();
    product.discontinue();

    assertThatThrownBy(product::resumeSupply)
        .isInstanceOf(InvalidProductStateChangeException.class);
    assertThat(product.getStatus()).isEqualTo(DISCONTINUED);
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
