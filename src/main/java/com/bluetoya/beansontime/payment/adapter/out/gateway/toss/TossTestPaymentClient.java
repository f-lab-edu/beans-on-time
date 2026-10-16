package com.bluetoya.beansontime.payment.adapter.out.gateway.toss;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** 테스트 시크릿 키만 허용하는 토스 승인·조회·전액 취소 HTTP 클라이언트다. */
public final class TossTestPaymentClient {
  private final RestClient client;

  public TossTestPaymentClient(String secretKey) {
    this(httpBuilder(), secretKey);
  }

  private static RestClient.Builder httpBuilder() {
    var http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    var requests = new JdkClientHttpRequestFactory(http);
    requests.setReadTimeout(Duration.ofSeconds(30));
    return RestClient.builder().requestFactory(requests);
  }

  TossTestPaymentClient(RestClient.Builder builder, String secretKey) {
    if (secretKey == null || !secretKey.matches("test_(?:g)?sk_[A-Za-z0-9]+")) {
      throw new IllegalArgumentException("토스 테스트 시크릿 키만 사용할 수 있습니다.");
    }
    client =
        builder
            .baseUrl("https://api.tosspayments.com")
            .defaultHeaders(headers -> headers.setBasicAuth(secretKey, ""))
            .build();
  }

  public PaymentResponse confirm(ConfirmationRequest request, String idempotencyKey) {
    Objects.requireNonNull(request, "승인 요청은 필수입니다.");
    return response(
        client
            .post()
            .uri("/v1/payments/confirm")
            .header("Idempotency-Key", requireIdempotencyKey(idempotencyKey))
            .contentType(MediaType.APPLICATION_JSON)
            .body(request));
  }

  public PaymentResponse find(String paymentKey) {
    requirePaymentKey(paymentKey);
    return response(client.get().uri("/v1/payments/{paymentKey}", paymentKey));
  }

  public PaymentResponse findByOrderId(String orderId) {
    requireOrderId(orderId);
    return response(client.get().uri("/v1/payments/orders/{orderId}", orderId));
  }

  public PaymentResponse cancel(String paymentKey, String reason, String idempotencyKey) {
    requirePaymentKey(paymentKey);
    if (reason == null || reason.isBlank() || reason.length() > 200) {
      throw new IllegalArgumentException("취소 사유는 1~200자여야 합니다.");
    }
    return response(
        client
            .post()
            .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
            .header("Idempotency-Key", requireIdempotencyKey(idempotencyKey))
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("cancelReason", reason)));
  }

  private PaymentResponse response(RestClient.RequestHeadersSpec<?> request) {
    try {
      PaymentResponse response = request.retrieve().body(PaymentResponse.class);
      if (response == null) throw new TossTestApiException(0, "EMPTY_RESPONSE");
      return response;
    } catch (RestClientResponseException exception) {
      String code = "UNRECOGNIZED_RESPONSE";
      try {
        var error = exception.getResponseBodyAs(ErrorResponse.class);
        if (error != null && error.code() != null && error.code().matches("[A-Z0-9_]{1,100}")) {
          code = error.code();
        }
      } catch (RestClientException ignored) {
        // 본문 형식이 다르더라도 성공이나 명확한 결제 거절로 해석하지 않는다.
      }
      throw new TossTestApiException(exception.getStatusCode().value(), code);
    } catch (RestClientException exception) {
      throw new TossTestApiException(0, "UNCONFIRMED_RESPONSE");
    }
  }

  private static String requireIdempotencyKey(String key) {
    if (key == null || !key.matches("[A-Za-z0-9_-]{1,300}")) {
      throw new IllegalArgumentException("멱등키는 1~300자의 영문·숫자·하이픈·밑줄이어야 합니다.");
    }
    return key;
  }

  private static void requirePaymentKey(String key) {
    if (key == null || key.isBlank() || key.length() > 200) {
      throw new IllegalArgumentException("결제 키는 1~200자여야 합니다.");
    }
  }

  private static void requireOrderId(String orderId) {
    if (orderId == null || !orderId.matches("[A-Za-z0-9_-]{6,64}")) {
      throw new IllegalArgumentException("PG 주문번호 형식이 올바르지 않습니다.");
    }
  }

  public record ConfirmationRequest(String paymentKey, String orderId, int amount) {
    public ConfirmationRequest {
      requirePaymentKey(paymentKey);
      requireOrderId(orderId);
      if (amount < 0) throw new IllegalArgumentException("승인 금액은 음수일 수 없습니다.");
    }
  }

  public record PaymentResponse(
      String paymentKey,
      String orderId,
      String type,
      String currency,
      String status,
      Integer totalAmount,
      Integer balanceAmount,
      String lastTransactionKey,
      OffsetDateTime approvedAt,
      List<CancelResponse> cancels) {
    public PaymentResponse {
      cancels = cancels == null ? List.of() : List.copyOf(cancels);
    }
  }

  public record CancelResponse(
      String transactionKey,
      Integer cancelAmount,
      String cancelStatus,
      OffsetDateTime canceledAt) {}

  private record ErrorResponse(String code) {}
}
