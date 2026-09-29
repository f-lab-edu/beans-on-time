package com.bluetoya.beansontime.payment.application.port.out;

import java.time.LocalDateTime;
import java.util.Objects;

public record PaymentGatewayResult(
    boolean successful, String transactionId, LocalDateTime completedAt) {

  public PaymentGatewayResult {
    Objects.requireNonNull(completedAt, "결제 결과 시각은 필수입니다.");
    if (successful && (transactionId == null || transactionId.isBlank())) {
      throw new IllegalArgumentException("승인된 결제의 거래 식별자는 필수입니다.");
    }
    if (!successful && transactionId != null) {
      throw new IllegalArgumentException("거절된 결제에는 거래 식별자가 없어야 합니다.");
    }
  }

  public static PaymentGatewayResult approved(String transactionId, LocalDateTime completedAt) {
    return new PaymentGatewayResult(true, transactionId, completedAt);
  }

  public static PaymentGatewayResult declined(LocalDateTime completedAt) {
    return new PaymentGatewayResult(false, null, completedAt);
  }
}
