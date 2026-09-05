package com.bluetoya.beansontime.billing.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PreparedBillingDetail(
    long billingId, int amount, LocalDate billingDate, String status, LocalDateTime createdAt) {}
