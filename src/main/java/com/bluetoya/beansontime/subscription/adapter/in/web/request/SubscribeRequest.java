package com.bluetoya.beansontime.subscription.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public record SubscribeRequest(@Positive long productId, @Valid CycleRequest cycle) {}
