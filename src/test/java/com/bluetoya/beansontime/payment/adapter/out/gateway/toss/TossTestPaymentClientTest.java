package com.bluetoya.beansontime.payment.adapter.out.gateway.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TossTestPaymentClientTest {
  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final TossTestPaymentClient client =
      new TossTestPaymentClient(builder, "test_sk_fixture");

  private static final String APPROVED =
      """
      {"paymentKey":"test-payment-key","orderId":"beans-payment-123","type":"NORMAL",
       "currency":"KRW","status":"DONE","totalAmount":18000,"balanceAmount":18000,
       "lastTransactionKey":"approval-transaction","approvedAt":"2026-10-03T10:00:00+09:00",
       "cancels":null,"futureField":"새로운 응답 필드"}
      """;

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "live_sk_fixture",
        "live_gsk_fixture",
        "test_ck_fixture",
        "test_gck_fixture",
        " test_sk_fixture",
        "test_sk_"
      })
  void rejectsLiveAndInvalidKeysBeforeSendingAnything(String key) {
    assertThatThrownBy(() -> new TossTestPaymentClient(RestClient.builder(), key))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("토스 테스트 시크릿 키만 사용할 수 있습니다.");
  }

  @Test
  void confirmsUsingBasicAuthenticationAndTheSuppliedIdempotencyKey() {
    server
        .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            header(
                "Authorization",
                "Basic "
                    + Base64.getEncoder()
                        .encodeToString("test_sk_fixture:".getBytes(StandardCharsets.UTF_8))))
        .andExpect(header("Idempotency-Key", "confirm-123"))
        .andExpect(
            content()
                .json(
                    """
            {"paymentKey":"test-payment-key","orderId":"beans-payment-123","amount":18000}
            """))
        .andRespond(withSuccess(APPROVED, MediaType.APPLICATION_JSON));
    var response =
        client.confirm(
            new TossTestPaymentClient.ConfirmationRequest(
                "test-payment-key", "beans-payment-123", 18000),
            "confirm-123");
    assertThat(response.status()).isEqualTo("DONE");
    assertThat(response.paymentKey()).isEqualTo("test-payment-key");
    assertThat(response.lastTransactionKey()).isEqualTo("approval-transaction");
    assertThat(response.approvedAt().toInstant()).isEqualTo("2026-10-03T01:00:00Z");
    assertThat(response.cancels()).isEmpty();
    server.verify();
  }

  @Test
  void queriesExistingPaymentWithoutSendingAnotherConfirmation() {
    server
        .expect(requestTo("https://api.tosspayments.com/v1/payments/test-payment-key"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(APPROVED, MediaType.APPLICATION_JSON));
    assertThat(client.find("test-payment-key").totalAmount()).isEqualTo(18000);
    server.verify();
  }

  @Test
  void queriesByPreviouslyStoredOrderId() {
    server
        .expect(requestTo("https://api.tosspayments.com/v1/payments/orders/beans-payment-123"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(APPROVED, MediaType.APPLICATION_JSON));
    assertThat(client.findByOrderId("beans-payment-123").paymentKey())
        .isEqualTo("test-payment-key");
    server.verify();
  }

  @Test
  void fullCancellationUsesSameKeyAndPreservesSeparateCancellationTransaction() {
    server
        .expect(requestTo("https://api.tosspayments.com/v1/payments/test-payment-key/cancel"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Idempotency-Key", "cancel-123"))
        .andExpect(content().json("{\"cancelReason\":\"내부 반영 실패 보상 취소\"}"))
        .andRespond(
            withSuccess(
                """
            {"paymentKey":"test-payment-key","orderId":"beans-payment-123","type":"NORMAL",
             "currency":"KRW","status":"CANCELED","totalAmount":18000,"balanceAmount":0,
             "lastTransactionKey":"cancel-transaction","approvedAt":"2026-10-03T10:00:00+09:00",
             "cancels":[{"transactionKey":"cancel-transaction","cancelAmount":18000,
              "cancelStatus":"DONE","canceledAt":"2026-10-03T10:01:00+09:00"}]}
            """,
                MediaType.APPLICATION_JSON));
    var response = client.cancel("test-payment-key", "내부 반영 실패 보상 취소", "cancel-123");
    assertThat(response.status()).isEqualTo("CANCELED");
    assertThat(response.totalAmount()).isEqualTo(18000);
    assertThat(response.cancels().getFirst().transactionKey()).isEqualTo("cancel-transaction");
    server.verify();
  }

  @Test
  void reportsApiErrorsWithoutReturningAFailedPaymentOrLeakingResponseBody() {
    server
        .expect(anything())
        .andRespond(
            withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"UNAUTHORIZED_KEY\",\"message\":\"private-value\"}"));
    assertThatThrownBy(() -> client.find("test-payment-key"))
        .isInstanceOfSatisfying(
            TossTestApiException.class,
            e -> {
              assertThat(e.statusCode()).isEqualTo(401);
              assertThat(e.code()).isEqualTo("UNAUTHORIZED_KEY");
              assertThat(e.getMessage()).doesNotContain("private-value", "test_sk_fixture");
              assertThat(e.getCause()).isNull();
            });
    server.verify();
  }

  @Test
  void doesNotRetryNetworkFailuresOrAssumeThatPaymentWasDeclined() {
    server.expect(anything()).andRespond(withException(new IOException("connection lost")));
    assertThatThrownBy(
            () ->
                client.confirm(
                    new TossTestPaymentClient.ConfirmationRequest(
                        "test-payment-key", "beans-payment-123", 18000),
                    "confirm-123"))
        .isInstanceOfSatisfying(
            TossTestApiException.class,
            e -> assertThat(e.code()).isEqualTo("UNCONFIRMED_RESPONSE"));
    server.verify();
  }

  @Test
  void rejectsEmptySuccessfulResponseAsUnconfirmed() {
    server.expect(anything()).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
    assertThatThrownBy(() -> client.find("test-payment-key"))
        .isInstanceOfSatisfying(
            TossTestApiException.class, e -> assertThat(e.code()).isEqualTo("EMPTY_RESPONSE"));
    server.verify();
  }
}
