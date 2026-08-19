package com.bluetoya.beansontime.customer.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class CustomerIdTest {

  @Test
  void 고객_아이디_생성시_값이_1보다_작으면_예외를_던진다() {
    assertThatThrownBy(() -> new CustomerId(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
