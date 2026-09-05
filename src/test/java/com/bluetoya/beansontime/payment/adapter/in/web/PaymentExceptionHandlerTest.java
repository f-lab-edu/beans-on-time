package com.bluetoya.beansontime.payment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.payment.application.exception.PaymentAlreadyAttemptedException;
import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentExceptionHandlerTest {

  @Test
  void mapsARepeatedPaymentAttemptToConflict() {
    PaymentExceptionHandler handler = new PaymentExceptionHandler();

    assertThat(
            handler
                .handlePaymentAlreadyAttempted(
                    new PaymentAlreadyAttemptedException("already attempted"))
                .getStatus())
        .isEqualTo(HttpStatus.CONFLICT.value());
  }

  @Test
  void mapsGatewayUnavailabilityToServiceUnavailable() {
    PaymentExceptionHandler handler = new PaymentExceptionHandler();

    assertThat(
            handler
                .handlePaymentGatewayUnavailable(
                    new PaymentGatewayUnavailableException("gateway timeout"))
                .getStatus())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
  }
}
