package com.bluetoya.beansontime.payment.adapter.config;

import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.billing.application.port.out.*;
import com.bluetoya.beansontime.payment.adapter.out.gateway.toss.*;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.application.service.*;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
@Profile("toss-test")
public class TossTestPaymentConfig {
  @Bean
  TossTestPaymentClient tossClient(@Value("${payment.toss.secret-key:}") String key) {
    return new TossTestPaymentClient(key);
  }

  @Bean
  TossTestPaymentGatewayAdapter tossGateway(
      TossTestPaymentClient client,
      PaymentCheckoutPort checkouts,
      LoadPaymentPort payments,
      Clock clock) {
    return new TossTestPaymentGatewayAdapter(client, checkouts, payments, clock);
  }

  @Bean
  @Primary
  PaymentCompletion recoverableCompletion(
      LoadPaymentPort payments,
      SavePaymentPort savePayments,
      LoadBillingPort billings,
      SaveBillingPort saveBillings,
      LoadSubscriptionPort subscriptions,
      BillingExecutionPort execution,
      PaymentApprovalPort approvals,
      PaymentCommitPort commits,
      CancelGatewayPaymentPort cancellations,
      PaymentCompletionService completion,
      Clock clock) {
    return new RecoverablePaymentCompletionService(
        payments,
        savePayments,
        billings,
        saveBillings,
        subscriptions,
        execution,
        approvals,
        commits,
        cancellations,
        completion,
        clock);
  }

  @Bean
  PreparePaymentCheckoutService prepareCheckout(
      OwnedBillingLoader billings,
      BillingExecutionPort execution,
      PaymentCheckoutPort checkouts,
      FindProcessingPaymentPort payments,
      Clock clock) {
    return new PreparePaymentCheckoutService(billings, execution, checkouts, payments, clock);
  }
}
