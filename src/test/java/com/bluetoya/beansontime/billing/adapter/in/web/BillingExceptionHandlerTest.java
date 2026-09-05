package com.bluetoya.beansontime.billing.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.billing.application.exception.BillingAlreadyPaidException;
import com.bluetoya.beansontime.billing.application.exception.BillingNotFoundException;
import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BillingExceptionHandlerTest {

  private final BillingExceptionHandler handler = new BillingExceptionHandler();

  @Test
  void mapsMissingBillingAndProductToNotFound() {
    assertThat(handler.handleBillingNotFound(new BillingNotFoundException("missing")).getStatus())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(handler.handleProductNotFound(new ProductNotFoundException("missing")).getStatus())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void mapsInvalidReactivationStateAndAlreadyPaidBillingToConflict() {
    assertThat(
            handler
                .handleReactivationBillingNotAllowed(
                    new ReactivationBillingNotAllowedException("not allowed"))
                .getStatus())
        .isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(
            handler
                .handleBillingAlreadyPaid(new BillingAlreadyPaidException("already paid"))
                .getStatus())
        .isEqualTo(HttpStatus.CONFLICT.value());
  }
}
