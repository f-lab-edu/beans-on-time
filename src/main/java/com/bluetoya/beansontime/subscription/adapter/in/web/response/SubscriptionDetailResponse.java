package com.bluetoya.beansontime.subscription.adapter.in.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record SubscriptionDetailResponse(
    SubscriptionResponse subscription, ProductResponse product) {

  public record SubscriptionResponse(
      String subscriptionId,
      long customerId,
      String deliveryCycleUnit,
      int deliveryCycleInterval,
      String lifecycleStatus,
      Set<String> suspensionReasons,
      LocalDate startedDate,
      LocalDate currentPeriodStartDate,
      LocalDate currentPeriodEndDate,
      Integer remainingPaidDays,
      int billingAnchorDay,
      LocalDate nextBillingDate,
      LocalDateTime pausedAt,
      LocalDate scheduledResumeDate,
      boolean executionBlocked) {}

  public record ProductResponse(
      String availability, long productId, String name, Integer basePrice) {}
}
