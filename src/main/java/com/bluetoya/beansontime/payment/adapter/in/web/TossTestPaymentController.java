package com.bluetoya.beansontime.payment.adapter.in.web;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.port.in.*;
import com.bluetoya.beansontime.payment.domain.PaymentAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("toss-test")
public class TossTestPaymentController {
  private final PreparePaymentCheckoutUseCase prepare;
  private final PayBillingUseCase pay;
  private final String clientKey;

  public TossTestPaymentController(
      PreparePaymentCheckoutUseCase prepare,
      PayBillingUseCase pay,
      @Value("${payment.toss.client-key:}") String clientKey) {
    if (!clientKey.matches("test_ck_[A-Za-z0-9]+"))
      throw new IllegalArgumentException("토스 개별 연동 테스트 클라이언트 키가 필요합니다.");
    this.prepare = prepare;
    this.pay = pay;
    this.clientKey = clientKey;
  }

  @GetMapping(value = "/billings/toss-test", produces = "text/html")
  org.springframework.core.io.Resource page() {
    return new org.springframework.core.io.ClassPathResource("payment/toss-test.html");
  }

  @PostMapping("/billings/{billingId}/payment-checkout")
  CheckoutResponse prepare(@PathVariable @Positive long billingId) {
    var detail = prepare.prepare(new BillingId(billingId));
    return new CheckoutResponse(detail.orderId(), detail.amount(), clientKey);
  }

  @PostMapping("/billings/{billingId}/payments")
  ResponseEntity<PaymentResponse> confirm(
      @PathVariable @Positive long billingId, @RequestBody @Valid Confirmation body) {
    return PaymentResponse.toResponse(
        pay.pay(
            new PayBillingCommand(
                new BillingId(billingId),
                new PaymentAuthorization(body.orderId(), body.paymentKey()))));
  }

  public record CheckoutResponse(String orderId, int amount, String clientKey) {}

  public record Confirmation(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{6,64}") String orderId,
      @NotBlank @Size(max = 200) String paymentKey) {
    @Override
    public String toString() {
      return "Confirmation[인증 정보 숨김]";
    }
  }
}
