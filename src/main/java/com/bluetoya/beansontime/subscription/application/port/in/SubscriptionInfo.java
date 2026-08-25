package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record SubscriptionInfo(
    String subscriptionId,
    long customerId,
    String cycleUnit,
    int cycleInterval,
    String lifecycleStatus,
    Set<SubscriptionSuspensionReason> suspensionReasons,
    LocalDate startedDate,
    LocalDate currentPeriodStartDate,
    LocalDate currentPeriodEndDate,
    int billingAnchorDay,
    LocalDate nextBillingDate,
    LocalDateTime pausedAt,
    LocalDate resumeDate,
    boolean executionBlocked) {

  public SubscriptionInfo {
    suspensionReasons = Set.copyOf(suspensionReasons);
  }
}
