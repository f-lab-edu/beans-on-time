package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SubscriptionExecutionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PauseSubscriptionService implements PauseSubscriptionUseCase {
  private final OwnedSubscriptionLoader ownedSubscriptionLoader;
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final Clock clock;

  private final SubscriptionExecutionPort subscriptionExecutionPort;

  @Override
  public void pause(PauseSubscriptionCommand command) {
    subscriptionExecutionPort.execute(
        command.subscriptionId(),
        () -> {
          apply(command);
          return null;
        });
  }

  private void apply(PauseSubscriptionCommand command) {
    Subscription subscription = ownedSubscriptionLoader.load(command.subscriptionId());

    subscription.pause(command.pauseUntilDate(), LocalDateTime.now(clock));

    saveSubscriptionPort.save(subscription);
  }
}
