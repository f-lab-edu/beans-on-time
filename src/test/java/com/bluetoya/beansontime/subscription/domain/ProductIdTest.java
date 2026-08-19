package com.bluetoya.beansontime.subscription.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ProductIdTest {
  @Test
  void 상품_아이디_생성시_값이_1보다_작으면_예외를_던진다() {
    assertThatThrownBy(() -> new ProductId(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
