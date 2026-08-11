package com.bluetoya.beansontime.subscription.application.port.in;

public interface CreateSubscriptionUseCase {
    void subscribe(CreateSubscriptionCommand command);
}
