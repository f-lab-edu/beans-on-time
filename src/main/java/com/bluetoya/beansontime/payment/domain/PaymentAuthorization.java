package com.bluetoya.beansontime.payment.domain;

public record PaymentAuthorization(String orderId, String paymentKey) {
  public PaymentAuthorization {
    if (orderId == null
        || !orderId.matches("[A-Za-z0-9_-]{6,64}")
        || paymentKey == null
        || paymentKey.isBlank()
        || paymentKey.length() > 200) throw new IllegalArgumentException("결제 인증 정보가 올바르지 않습니다.");
  }

  @Override
  public String toString() {
    return "PaymentAuthorization[인증 정보 숨김]";
  }
}
