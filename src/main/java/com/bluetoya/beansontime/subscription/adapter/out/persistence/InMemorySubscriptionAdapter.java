package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemorySubscriptionAdapter implements SaveSubscriptionPort {
    private final List<Subscription> subscriptions =
            new CopyOnWriteArrayList<>();

    @Override
    public void save(Subscription subscription) {
        subscriptions.add(subscription);
    }
}
