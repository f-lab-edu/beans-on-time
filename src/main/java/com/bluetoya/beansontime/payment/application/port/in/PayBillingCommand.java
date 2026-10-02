package com.bluetoya.beansontime.payment.application.port.in;

import com.bluetoya.beansontime.billing.domain.BillingId;
import java.util.Objects;

public record PayBillingCommand(BillingId billingId) {
  public PayBillingCommand {
    Objects.requireNonNull(billingId, "청구 ID는 필수입니다.");
  }
}
