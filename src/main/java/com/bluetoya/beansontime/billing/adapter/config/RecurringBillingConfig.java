package com.bluetoya.beansontime.billing.adapter.config;

import com.bluetoya.beansontime.billing.application.port.in.*;
import com.bluetoya.beansontime.billing.application.port.out.*;
import com.bluetoya.beansontime.billing.application.service.*;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.application.service.PaymentAttemptSubmission;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.subscription.application.port.out.*;
import java.time.Clock;
import org.springframework.context.annotation.*;

/**
 * 토스 자동결제를 아직 지원하지 않으므로 toss-test에서는 정기결제 서비스 전체를 제외한다. 애플리케이션 서비스에 프로필 조건을 두지 않고 이 설정에서 관련 빈을 함께
 * 등록한다.
 */
@Configuration
@Profile("!toss-test")
public class RecurringBillingConfig {
  @Bean
  ProcessRecurringBillingService recurringBilling(
      LoadSubscriptionPort subscriptions,
      LoadProductPort products,
      FindRecurringBillingPort recurring,
      FindPendingBillingPort pending,
      SaveBillingPort saveBillings,
      BillingExecutionPort execution,
      ExistsPaymentAttemptPort attempts,
      SavePaymentPort savePayments,
      PaymentAttemptSubmission submission,
      Clock clock) {
    return new ProcessRecurringBillingService(
        subscriptions,
        products,
        recurring,
        pending,
        saveBillings,
        execution,
        attempts,
        savePayments,
        submission,
        clock);
  }

  @Bean
  RunRecurringBillingUseCase recurringRun(
      FindDueSubscriptionsPort candidates, ProcessRecurringBillingService process, Clock clock) {
    return new RunRecurringBillingService(candidates, process, clock);
  }
}
