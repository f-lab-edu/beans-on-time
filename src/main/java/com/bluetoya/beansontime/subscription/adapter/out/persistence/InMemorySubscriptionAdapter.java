package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionQueryPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySubscriptionAdapter implements SaveSubscriptionPort, LoadSubscriptionQueryPort {
    private final Map<SubscriptionId, Subscription> subscriptions =
            new ConcurrentHashMap<>();

    @Override
    public void save(Subscription subscription) {
        subscriptions.put(
                subscription.getSubscriptionId(),
                subscription
        );
    }

    @Override
    public SubscriptionQueryResult findById(SubscriptionId subscriptionId) {
        Subscription subscription =
                subscriptions.get(subscriptionId);

        if (subscription == null) {
            throw new IllegalArgumentException(
                    "구독 내역을 찾을 수 없습니다."
            );
        }

        return new SubscriptionQueryResult(
                subscription.getSubscriptionId(),
                subscription.getCustomerId(),
                subscription.getProductId(),
                subscription.getCycle(),
                subscription.getSubscriptionStatus()
        );
    }

}
