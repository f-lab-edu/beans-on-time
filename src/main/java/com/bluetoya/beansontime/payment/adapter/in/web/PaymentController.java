package com.bluetoya.beansontime.payment.adapter.in.web;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingCommand;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingUseCase;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@org.springframework.context.annotation.Profile("!toss-test")
@RestController
@RequiredArgsConstructor
public class PaymentController {
  private final PayBillingUseCase payBillingUseCase;

  @PostMapping("/billings/{billingId}/payments")
  ResponseEntity<PaymentResponse> pay(
      @PathVariable @Positive(message = "청구 ID는 0보다 커야 합니다.") long billingId) {
    PaymentResult result = payBillingUseCase.pay(new PayBillingCommand(new BillingId(billingId)));
    return PaymentResponse.toResponse(result);
  }
}
