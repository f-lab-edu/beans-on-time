package com.bluetoya.beansontime.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProductIdTest {
  @ParameterizedTest
  @ValueSource(longs = {Long.MIN_VALUE, -1, 0})
  void rejectsNonPositiveIds(long id) {
    assertThatThrownBy(() -> new ProductId(id)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsPositiveBoundaryIds() {
    assertThat(new ProductId(1).id()).isEqualTo(1);
    assertThat(new ProductId(Long.MAX_VALUE).id()).isEqualTo(Long.MAX_VALUE);
  }
}
