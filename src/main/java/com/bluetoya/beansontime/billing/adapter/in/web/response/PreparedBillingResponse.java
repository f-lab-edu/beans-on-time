package com.bluetoya.beansontime.billing.adapter.in.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PreparedBillingResponse(
    long billingId, int amount, LocalDate billingDate, String status, LocalDateTime createdAt) {}
