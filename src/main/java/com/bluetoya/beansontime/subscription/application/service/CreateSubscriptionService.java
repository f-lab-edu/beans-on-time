package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateSubscriptionService implements CreateSubscriptionUseCase {
    private final SaveSubscriptionPort saveSubscriptionPort;
    private final ExistsSubscriptionPort existsSubscriptionPort;

    @Override
    public void subscribe(CreateSubscriptionCommand command) {
        if (existsSubscriptionPort.isExists(command.customerId(), command.productId())) {
            throw new IllegalArgumentException("중복 구독은 불가능합니다.");
        }

        Subscription subscription = new Subscription(command.customerId(), command.productId(), command.cycle());
        saveSubscriptionPort.save(subscription);
    }
}
