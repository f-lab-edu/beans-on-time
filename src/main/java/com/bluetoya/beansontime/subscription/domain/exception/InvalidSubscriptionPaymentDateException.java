package com.bluetoya.beansontime.subscription.domain.exception;

/** 외부 승인일과 구독 일정이 충돌하여 내부 확인이 필요한 상태다. */
public class InvalidSubscriptionPaymentDateException extends IllegalStateException {
  public InvalidSubscriptionPaymentDateException(String message) {
    super(message);
  }
}
