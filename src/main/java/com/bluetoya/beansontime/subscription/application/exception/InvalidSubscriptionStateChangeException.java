package com.bluetoya.beansontime.subscription.application.exception;

public class InvalidSubscriptionStateChangeException extends RuntimeException {
    public InvalidSubscriptionStateChangeException(String message) {
        super(message);
    }
}
