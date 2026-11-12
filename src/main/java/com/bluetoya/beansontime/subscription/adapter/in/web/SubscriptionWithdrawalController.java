package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.subscription.application.port.in.*;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions/{id}/withdrawal")
@RequiredArgsConstructor
public class SubscriptionWithdrawalController {
  private final WithdrawSubscriptionUseCase withdraw;
  private final GetWithdrawalQuery query;

  @PostMapping
  public WithdrawalResult withdraw(@PathVariable UUID id) {
    return withdraw.withdraw(new SubscriptionId(id));
  }

  @GetMapping
  public WithdrawalResult find(@PathVariable UUID id) {
    return query.find(new SubscriptionId(id));
  }
}
