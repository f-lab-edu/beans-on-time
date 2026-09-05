package com.bluetoya.beansontime.billing.application.port.in;

import java.time.LocalDate;

public record BillingCheckoutDetail(
    long customerId, SubscriptionInfo subscription, ProductInfo product, BillingInfo billing) {

  public record SubscriptionInfo(String subscriptionId, String lifecycleStatus) {}

  public record ProductInfo(long productId, String name) {}

  public record BillingInfo(long billingId, int amount, LocalDate billingDate, String status) {}
}
