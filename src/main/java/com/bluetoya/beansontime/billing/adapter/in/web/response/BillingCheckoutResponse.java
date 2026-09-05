package com.bluetoya.beansontime.billing.adapter.in.web.response;

import java.time.LocalDate;

public record BillingCheckoutResponse(
    SubscriptionResponse subscription, ProductResponse product, BillingResponse billing) {

  public record SubscriptionResponse(String subscriptionId, String lifecycleStatus) {}

  public record ProductResponse(long productId, String name) {}

  public record BillingResponse(long billingId, int amount, LocalDate billingDate, String status) {}
}
