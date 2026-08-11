package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionQueryPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.domain.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySubscriptionAdapter implements SaveSubscriptionPort, LoadSubscriptionQueryPort, ExistsSubscriptionPort {
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

        if (Objects.isNull(subscription)) {
            throw new IllegalArgumentException(
                    "구독 내역을 찾을 수 없습니다."
            );
        }

        return new SubscriptionQueryResult(
                subscription.getSubscriptionId().value(),
                subscription.getCustomerId().id(),
                subscription.getProductId().id(),
                subscription.getCycle().getUnit().name(),
                subscription.getCycle().getInterval(),
                subscription.getSubscriptionStatus().name()
        );
    }

    @Override
    public boolean isExists(CustomerId customerId, ProductId productId) {
        return subscriptions.values().stream()
                .anyMatch(subscription ->
                        subscription.getCustomerId().equals(customerId)
                                && subscription.getProductId().equals(productId)
                                && subscription.getSubscriptionStatus()
                                != SubscriptionStatus.CANCEL
                );
    }
}
