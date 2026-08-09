package com.bluetoya.beansontime.subscription.application.port;

import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateSubscriptionService implements CreateSubscriptionUseCase {
    private final SaveSubscriptionPort saveSubscriptionPort;

    @Override
    public void subscribe(CreateSubscriptionCommand command) {
        Subscription subscription = new Subscription(command.customerId(), command.productId(), command.cycle());
        saveSubscriptionPort.save(subscription);
    }
}
