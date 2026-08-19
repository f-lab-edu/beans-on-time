package com.bluetoya.beansontime.subscription.application.port.in;

public interface PauseSubscriptionUseCase {
  void pause(PauseSubscriptionCommand command);
}
