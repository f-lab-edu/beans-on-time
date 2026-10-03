package com.bluetoya.beansontime.payment.adapter.out.gateway.toss;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.product.domain.Money;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TossTestPaymentGatewayAdapterTest {
  private final LocalDateTime now = LocalDateTime.of(2026, 10, 3, 10, 0);
  private final TossTestPaymentClient client = mock(TossTestPaymentClient.class);
  private final Payment payment = Payment.start(new BillingId(1), new Money(10000), now);
  private final PaymentAuthorization auth =
      new PaymentAuthorization("order_test_123", "payment_key");
  private final PaymentCheckoutPort checkouts = mock(PaymentCheckoutPort.class);
  private final LoadPaymentPort payments = id -> Optional.of(payment);

  private TossTestPaymentGatewayAdapter gateway(LocalDateTime at) {
    when(checkouts.load(payment.getId())).thenReturn(auth);
    return new TossTestPaymentGatewayAdapter(
        client,
        checkouts,
        payments,
        Clock.fixed(at.toInstant(ZoneOffset.ofHours(9)), ZoneId.of("Asia/Seoul")));
  }

  private TossTestPaymentClient.PaymentResponse response(String status) {
    return new TossTestPaymentClient.PaymentResponse(
        auth.paymentKey(),
        auth.orderId(),
        "NORMAL",
        "KRW",
        status,
        10000,
        10000,
        "approved_tx",
        now.atOffset(ZoneOffset.ofHours(9)),
        List.of());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "READY",
        "IN_PROGRESS",
        "WAITING_FOR_DEPOSIT",
        "PARTIAL_CANCELED",
        "CANCELED",
        "NEW_STATUS"
      })
  void unknownOrNonFinalResultsDoNotGrantEntitlement(String status) {
    when(client.find(auth.paymentKey())).thenReturn(response(status));
    assertThat(gateway(now).find(payment.getId())).isEmpty();
  }

  @Test
  void rejectsMismatchedIdentityAndAmountEvenWhenDone() {
    when(client.find(auth.paymentKey()))
        .thenReturn(
            new TossTestPaymentClient.PaymentResponse(
                auth.paymentKey(),
                "another_order",
                "NORMAL",
                "KRW",
                "DONE",
                1,
                1,
                "tx",
                now.atOffset(ZoneOffset.ofHours(9)),
                List.of()));
    assertThatThrownBy(() -> gateway(now).find(payment.getId()))
        .isInstanceOf(PaymentGatewayUnavailableException.class);
  }

  @Test
  void convertsPgApprovalOffsetToKst() {
    when(client.find(auth.paymentKey()))
        .thenReturn(
            new TossTestPaymentClient.PaymentResponse(
                auth.paymentKey(),
                auth.orderId(),
                "NORMAL",
                "KRW",
                "DONE",
                10000,
                10000,
                "tx",
                OffsetDateTime.parse("2026-10-02T16:30:00Z"),
                List.of()));
    assertThat(gateway(now).find(payment.getId()).orElseThrow().completedAt())
        .isEqualTo(LocalDateTime.of(2026, 10, 3, 1, 30));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ABORTED", "EXPIRED"})
  void onlyVerifiedTerminalUnapprovedResultsFailTheAttempt(String status) {
    when(client.find(auth.paymentKey()))
        .thenReturn(
            new TossTestPaymentClient.PaymentResponse(
                auth.paymentKey(),
                auth.orderId(),
                "NORMAL",
                "KRW",
                status,
                10000,
                10000,
                null,
                null,
                List.of()));
    assertThat(gateway(now).find(payment.getId()).orElseThrow().successful()).isFalse();
  }

  @Test
  void authorizationErrorsRemainUnconfirmed() {
    when(client.find(auth.paymentKey()))
        .thenThrow(new TossTestApiException(401, "UNAUTHORIZED_KEY"));
    assertThatThrownBy(() -> gateway(now).find(payment.getId()))
        .isInstanceOf(PaymentGatewayUnavailableException.class);
  }

  @Test
  void doesNotResendCancellationAtIdempotencyExpiry() {
    var approval =
        PaymentApproval.observed(payment.getId(), "approved_tx", now)
            .beginApplication(now)
            .decideCancellation(now);
    when(client.find(auth.paymentKey())).thenReturn(response("DONE"));
    assertThat(gateway(now.plusDays(15)).cancelOrFind(payment, approval)).isEmpty();
    verify(client, never()).cancel(anyString(), anyString(), anyString());
  }

  @Test
  void repeatsOnlyThePersistedCancellationKeyAndPayload() {
    var approval =
        PaymentApproval.observed(payment.getId(), "approved_tx", now)
            .beginApplication(now)
            .decideCancellation(now);
    when(client.find(auth.paymentKey())).thenReturn(response("DONE"));
    when(client.cancel(anyString(), anyString(), anyString()))
        .thenThrow(new TossTestApiException(0, "UNCONFIRMED_RESPONSE"));
    var gateway = gateway(now);
    assertThatThrownBy(() -> gateway.cancelOrFind(payment, approval))
        .isInstanceOf(PaymentGatewayUnavailableException.class);
    assertThatThrownBy(() -> gateway.cancelOrFind(payment, approval))
        .isInstanceOf(PaymentGatewayUnavailableException.class);
    verify(client, times(2))
        .cancel(
            eq(auth.paymentKey()), eq("구독 활성화 DB 반영 실패에 따른 전액 보상 취소"), eq(approval.cancelKey()));
  }
}
