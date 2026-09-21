package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.product.domain.Money;
import java.util.Objects;

public record PaymentGatewayRequest(BillingId billingId, Money amount) {
  public PaymentGatewayRequest {
    Objects.requireNonNull(billingId, "결제할 청구 ID는 필수입니다.");
    Objects.requireNonNull(amount, "결제 요청 금액은 필수입니다.");
  }
}
