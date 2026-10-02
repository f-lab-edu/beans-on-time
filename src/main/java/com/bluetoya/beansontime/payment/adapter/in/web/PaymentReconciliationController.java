package com.bluetoya.beansontime.payment.adapter.in.web;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.port.in.ReconcilePaymentCommand;
import com.bluetoya.beansontime.payment.application.port.in.ReconcilePaymentUseCase;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentReconciliationController {
  private final ReconcilePaymentUseCase reconcilePaymentUseCase;

  @PostMapping("/billings/{billingId}/payments/{paymentId}/reconcile")
  ResponseEntity<PaymentResponse> reconcile(
      @PathVariable @Positive long billingId, @PathVariable @Positive long paymentId) {
    return PaymentResponse.toResponse(
        reconcilePaymentUseCase.reconcile(
            new ReconcilePaymentCommand(new BillingId(billingId), new PaymentId(paymentId))));
  }
}
