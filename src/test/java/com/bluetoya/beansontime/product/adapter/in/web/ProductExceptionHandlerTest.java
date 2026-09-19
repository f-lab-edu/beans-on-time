package com.bluetoya.beansontime.product.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.domain.exception.InvalidSupplyStateChangeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ProductExceptionHandlerTest {

  private final ProductExceptionHandler handler = new ProductExceptionHandler();

  @Test
  void mapsProductNotFoundToNotFound() {
    ProblemDetail problem =
        handler.handleProductNotFound(new ProductNotFoundException("상품을 찾을 수 없습니다."));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void mapsInvalidSupplyStateChangeToConflict() {
    ProblemDetail problem =
        handler.handleInvalidSupplyStateChange(
            new InvalidSupplyStateChangeException("영구 종료된 상품입니다."));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
  }
}
