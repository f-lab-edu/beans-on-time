package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PauseSubscriptionService implements PauseSubscriptionUseCase {
  private final OwnedSubscriptionLoader ownedSubscriptionLoader;
  private final SaveSubscriptionPort saveSubscriptionPort;

  @Override
  public void pause(PauseSubscriptionCommand command) {
    Subscription subscription =
        ownedSubscriptionLoader.loadOwnedSubscription(command.subscriptionId());
    subscription.pause();
    saveSubscriptionPort.save(subscription);
  }
}
