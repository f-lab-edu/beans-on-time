package com.bluetoya.beansontime.payment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.payment.application.exception.PaymentInProgressException;
import com.bluetoya.beansontime.payment.application.exception.PaymentNotFoundException;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import org.junit.jupiter.api.Test;

class PaymentExceptionHandlerTest {
  @Test
  void returnsTheExistingPaymentIdWithAConflict() {
    var response =
        new PaymentExceptionHandler()
            .handlePaymentInProgress(new PaymentInProgressException(new PaymentId(3)));
    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(response.getProperties()).containsEntry("paymentId", 3L);
  }

  @Test
  void mapsMissingPaymentsToNotFound() {
    assertThat(
            new PaymentExceptionHandler()
                .handlePaymentNotFound(new PaymentNotFoundException("missing"))
                .getStatus())
        .isEqualTo(404);
  }
}
