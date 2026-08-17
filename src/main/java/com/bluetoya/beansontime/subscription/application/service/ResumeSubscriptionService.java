package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.ResumeSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.ResumeSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeSubscriptionService implements ResumeSubscriptionUseCase {
  private final OwnedSubscriptionLoader ownedSubscriptionLoader;
  private final SaveSubscriptionPort saveSubscriptionPort;

  @Override
  public void resume(ResumeSubscriptionCommand command) {
    Subscription subscription =
        ownedSubscriptionLoader.load(command.subscriptionId());

    subscription.resume();

    saveSubscriptionPort.save(subscription);
  }
}
