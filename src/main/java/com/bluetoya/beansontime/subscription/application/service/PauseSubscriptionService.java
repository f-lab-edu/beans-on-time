package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PauseSubscriptionService implements PauseSubscriptionUseCase {
    private final LoadSubscriptionPort loadSubscriptionPort;
    private final SaveSubscriptionPort saveSubscriptionPort;

    @Override
    public void pause(PauseSubscriptionCommand command) {
        Subscription subscription = loadSubscriptionPort
                .load(command.subscriptionId())
                .orElseThrow();

        subscription.pause();

        saveSubscriptionPort.save(subscription);
    }
}
