package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;

public record PauseSubscriptionCommand(SubscriptionId subscriptionId, LocalDate pauseUntilDate) {}
