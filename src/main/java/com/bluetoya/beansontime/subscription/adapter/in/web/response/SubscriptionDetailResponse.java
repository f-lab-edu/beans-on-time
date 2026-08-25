package com.bluetoya.beansontime.subscription.adapter.in.web.response;

public record SubscriptionDetailResponse(
    SubscriptionResponse subscription, ProductResponse product) {

  public record SubscriptionResponse(
      String subscriptionId, long customerId, String cycleUnit, int cycleInterval, String status) {}

  public record ProductResponse(
      String availability, long productId, String name, Integer basePrice) {}
}
