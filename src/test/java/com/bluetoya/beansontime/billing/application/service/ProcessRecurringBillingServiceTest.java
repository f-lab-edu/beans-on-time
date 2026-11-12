package com.bluetoya.beansontime.billing.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bluetoya.beansontime.billing.application.port.out.*;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.application.service.PaymentAttemptSubmission;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.*;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;

class ProcessRecurringBillingServiceTest {
  private final LoadSubscriptionPort subscriptions = mock(LoadSubscriptionPort.class);
  private final LoadProductPort products = mock(LoadProductPort.class);
  private final SaveBillingPort billings = mock(SaveBillingPort.class);
  private final SavePaymentPort payments = mock(SavePaymentPort.class);
  private final PaymentAttemptSubmission submission = mock(PaymentAttemptSubmission.class);
  private final BillingExecutionPort execution = mock(BillingExecutionPort.class);
  private static final LocalDate DATE = LocalDate.of(2026, 10, 4);
  private final ProcessRecurringBillingService service =
      new ProcessRecurringBillingService(
          subscriptions,
          products,
          mock(FindRecurringBillingPort.class),
          mock(FindPendingBillingPort.class),
          billings,
          execution,
          mock(ExistsPaymentAttemptPort.class),
          payments,
          submission,
          Clock.fixed(Instant.parse("2026-10-04T01:00:00Z"), ZoneId.of("Asia/Seoul")));

  @BeforeEach
  void executeAction() {
    when(execution.execute(any(), any()))
        .thenAnswer(call -> call.<Supplier<?>>getArgument(1).get());
  }

  @Test
  void missingCandidateReportsInternalFailureWithSubscriptionId() {
    var id = SubscriptionId.generate();
    when(subscriptions.load(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.process(id, DATE, DATE))
        .isExactlyInstanceOf(IllegalStateException.class)
        .hasMessageContaining("정기결제 대상으로 조회한 구독")
        .hasMessageContaining(id.value().toString());
    verifyNoInteractions(billings, payments, submission);
  }

  @Test
  void missingReferencedProductReportsBothIdentifiersBeforeCreatingAttempt() {
    var subscription =
        new Subscription(
            new CustomerId(1),
            new ProductId(2),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            DATE.minusMonths(1));
    when(subscriptions.load(subscription.getId())).thenReturn(Optional.of(subscription));
    when(products.load(subscription.getProductId())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.process(subscription.getId(), DATE, DATE))
        .isExactlyInstanceOf(IllegalStateException.class)
        .hasMessageContaining("참조하는 상품")
        .hasMessageContaining(subscription.getId().value().toString())
        .hasMessageContaining("productId=2");
    verifyNoInteractions(billings, payments, submission);
  }
}
