package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.port.in.GetSubscriptionQuery;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionQueryPort;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSubscriptionService implements GetSubscriptionQuery {
    private final LoadSubscriptionQueryPort loadSubscriptionQueryPort;

    @Override
    public SubscriptionQueryResult get(SubscriptionId subscriptionId) {
        return loadSubscriptionQueryPort.findById(subscriptionId);
    }
}
