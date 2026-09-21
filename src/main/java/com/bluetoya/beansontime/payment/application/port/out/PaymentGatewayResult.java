package com.bluetoya.beansontime.payment.application.port.out;

public record PaymentGatewayResult(boolean successful, String transactionId) {

  public PaymentGatewayResult {
    if (successful && (transactionId == null || transactionId.isBlank())) {
      throw new IllegalArgumentException("승인된 결제의 거래 식별자는 필수입니다.");
    }
    if (!successful && transactionId != null) {
      throw new IllegalArgumentException("거절된 결제에는 거래 식별자가 없어야 합니다.");
    }
  }

  public static PaymentGatewayResult approved(String transactionId) {
    return new PaymentGatewayResult(true, transactionId);
  }

  public static PaymentGatewayResult declined() {
    return new PaymentGatewayResult(false, null);
  }
}
