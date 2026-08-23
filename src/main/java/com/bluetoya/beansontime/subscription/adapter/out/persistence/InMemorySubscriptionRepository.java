package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionStatus;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySubscriptionRepository {
    private final Map<SubscriptionId, Subscription> subscriptions = new ConcurrentHashMap<>();

    public Optional<Subscription> findById(SubscriptionId subscriptionId) {
        return Optional.ofNullable(subscriptions.get(subscriptionId));
    }

    public void save(Subscription subscription) {
        subscriptions.put(subscription.getId(), subscription);
    }

    public boolean isExists(CustomerId customerId, ProductId productId) {
        return subscriptions.values().stream()
                .anyMatch(
                        subscription ->
                                subscription.getCustomerId().equals(customerId)
                                        && subscription.getProductId().equals(productId)
                                        && subscription.getSubscriptionStatus() != SubscriptionStatus.CANCEL);
    }

}
