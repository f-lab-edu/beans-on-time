package com.bluetoya.beansontime.payment.application.port.in;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.PaymentAuthorization;
import java.util.Objects;

public record PayBillingCommand(BillingId billingId, PaymentAuthorization authorization) {
  public PayBillingCommand(BillingId billingId) {
    this(billingId, null);
  }

  public PayBillingCommand {
    Objects.requireNonNull(billingId, "청구 ID는 필수입니다.");
  }
}
