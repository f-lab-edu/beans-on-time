package com.bluetoya.beansontime.payment.adapter.out.gateway.toss;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bluetoya.beansontime.billing.adapter.out.persistence.JdbcBillingAdapter;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.payment.adapter.out.persistence.*;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.product.adapter.out.persistence.JdbcProductAdapter;
import com.bluetoya.beansontime.product.domain.*;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.JdbcSubscriptionAdapter;
import com.bluetoya.beansontime.subscription.domain.*;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/** 카드 인증 이후의 입력부터 실제 HTTP 직렬화와 PostgreSQL 결과 반영까지 연결한다. */
@SpringBootTest(
    properties = {
      "payment.reconciliation.enabled=false",
      "payment.toss.client-key=test_ck_fixture",
      "payment.toss.secret-key=test_sk_fixture"
    })
@ActiveProfiles("toss-test")
@AutoConfigureMockMvc
@Testcontainers
@Import(TossHttpPaymentIntegrationTest.HttpConfig.class)
class TossHttpPaymentIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
  @Autowired MockMvc mvc;
  @Autowired JdbcClient jdbc;
  @Autowired JdbcProductAdapter products;
  @Autowired JdbcSubscriptionAdapter subscriptions;
  @Autowired JdbcBillingAdapter billings;
  @Autowired JdbcPaymentAdapter payments;
  @Autowired JdbcPaymentApprovalAdapter approvals;
  @Autowired LocalGateway gateway;
  private Billing billing;
  private String order;
  private String key;
  private final JsonMapper json = new JsonMapper();

  @TestConfiguration
  static class HttpConfig {
    @Bean
    LocalGateway localGateway() throws IOException {
      return new LocalGateway();
    }

    @Bean
    @Primary
    TossTestPaymentClient httpClient(LocalGateway gateway) {
      var factory = new JdkClientHttpRequestFactory();
      factory.setReadTimeout(Duration.ofSeconds(3));
      // 명시적 URI factory가 클라이언트의 baseUrl보다 우선한다. 실제 외부 호스트로 전송하지 않는다.
      var builder =
          RestClient.builder()
              .requestFactory(factory)
              .uriBuilderFactory(new DefaultUriBuilderFactory(gateway.url()));
      return new TossTestPaymentClient(builder, "test_sk_fixture");
    }

    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(NOW.toInstant(ZoneOffset.ofHours(9)), ZoneId.of("Asia/Seoul"));
    }
  }

  static class LocalGateway implements AutoCloseable {
    final HttpServer server;
    final Queue<Reply> replies = new ConcurrentLinkedQueue<>();
    final Queue<Request> requests = new ConcurrentLinkedQueue<>();

    LocalGateway() throws IOException {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            try (exchange) {
              requests.add(
                  new Request(
                      exchange.getRequestMethod(),
                      exchange.getRequestURI().getPath(),
                      exchange.getRequestHeaders().getFirst("Authorization"),
                      exchange.getRequestHeaders().getFirst("Idempotency-Key"),
                      new String(
                          exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
              var reply = replies.poll();
              if (reply == null) {
                exchange.sendResponseHeaders(500, -1);
                return;
              }
              var bytes = reply.body().getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().set("Content-Type", "application/json");
              exchange.sendResponseHeaders(reply.status(), bytes.length);
              exchange.getResponseBody().write(bytes);
            }
          });
      server.start();
    }

    String url() {
      return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void respond(String body) {
      replies.add(new Reply(200, body));
    }

    void uncertain() {
      replies.add(new Reply(503, "{\"code\":\"UNKNOWN_ERROR\"}"));
    }

    public void close() {
      server.stop(0);
    }
  }

  record Reply(int status, String body) {}

  record Request(
      String method, String path, String authorization, String idempotencyKey, String body) {}

  @BeforeEach
  void prepare() throws Exception {
    gateway.requests.clear();
    gateway.replies.clear();
    key = "test_payment_" + UUID.randomUUID();
    var product = new Product(new SellerId(1), "HTTP 검증 원두", new Money(10000));
    products.saveNew(product);
    var subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            LocalDate.of(2026, 9, 1));
    subscription.pause(LocalDate.of(2026, 10, 10), LocalDateTime.of(2026, 9, 30, 10, 0));
    subscriptions.saveNew(subscription);
    var prepared =
        mvc.perform(
                post("/subscriptions/{id}/reactivation-billing", subscription.getId().value())
                    .with(httpBasic("customer1", "password1")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    billing =
        billings
            .load(new BillingId(json.readTree(prepared).get("billingId").asLong()))
            .orElseThrow();
    var checkout =
        mvc.perform(
                post("/billings/{id}/payment-checkout", billing.getId().value())
                    .with(httpBasic("customer1", "password1")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    order = json.readTree(checkout).get("orderId").asText();
  }

  @AfterEach
  void consumedAllResponses() {
    assertThat(gateway.replies).isEmpty();
    String basic =
        "Basic "
            + Base64.getEncoder()
                .encodeToString("test_sk_fixture:".getBytes(StandardCharsets.UTF_8));
    assertThat(gateway.requests).allSatisfy(r -> assertThat(r.authorization()).isEqualTo(basic));
  }

  @Test
  void approvalTravelsThroughHttpAndCommitsAllThreeAggregates() throws Exception {
    gateway.respond(response(false));
    var id = confirm(200, "SUCCESS");
    assertSuccess(id);
    reconcile(id, "SUCCESS");
    assertThat(gateway.requests).hasSize(1);
    assertConfirmation(gateway.requests.element());
  }

  @Test
  void uncertainApprovalRecoversByHttpQueryWithoutReconfirmation() throws Exception {
    gateway.uncertain();
    var id = confirm(202, "PROCESSING");
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PENDING);
    gateway.respond(response(false));
    reconcile(id, "SUCCESS");
    assertSuccess(id);
    assertThat(gateway.requests).extracting(Request::method).containsExactly("POST", "GET");
    assertConfirmation(gateway.requests.element());
    assertThat(new ArrayList<>(gateway.requests).get(1).path()).isEqualTo("/v1/payments/" + key);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void databaseRollbackCompensatesThroughHttpAndRecoversUncertainCancellation(boolean uncertain)
      throws Exception {
    gateway.respond(response(false));
    gateway.respond(response(false));
    if (uncertain) gateway.uncertain();
    else gateway.respond(response(true));
    jdbc.sql(
            "alter table subscriptions add constraint http_injected_failure check (id <> '"
                + billing.getSubscriptionId().value()
                + "'::uuid) not valid")
        .update();
    PaymentId id;
    try {
      id = confirm(uncertain ? 202 : 200, uncertain ? "CANCEL_PENDING" : "CANCELLED");
    } finally {
      jdbc.sql("alter table subscriptions drop constraint http_injected_failure").update();
    }
    if (uncertain) {
      assertThat(payments.load(id).orElseThrow().getStatus())
          .isEqualTo(PaymentStatus.CANCEL_PENDING);
      assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
          .isEqualTo(BillingStatus.PENDING);
      gateway.respond(response(true));
      reconcile(id, "CANCELLED");
    }
    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.CANCELLED);
    assertThat(subscriptions.load(billing.getSubscriptionId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.PAUSED);
    var approval = approvals.load(id).orElseThrow();
    assertThat(approval.phase()).isEqualTo(PaymentApproval.Phase.CANCELLED);
    assertThat(approval.transactionId()).isEqualTo("approval_" + order);
    assertThat(approval.cancelTransactionId()).isEqualTo("cancel_" + order);
    var requests = new ArrayList<>(gateway.requests);
    assertThat(requests).hasSize(uncertain ? 4 : 3);
    assertConfirmation(requests.getFirst());
    assertThat(requests.get(1).method()).isEqualTo("GET");
    assertThat(requests.get(1).path()).isEqualTo("/v1/payments/" + key);
    var cancel = requests.get(2);
    assertThat(cancel.method()).isEqualTo("POST");
    assertThat(cancel.path()).isEqualTo("/v1/payments/" + key + "/cancel");
    assertThat(cancel.idempotencyKey()).isEqualTo(approval.cancelKey());
    assertThat(json.readTree(cancel.body()).properties()).hasSize(1);
    assertThat(json.readTree(cancel.body()).get("cancelReason").asText())
        .isEqualTo("구독 활성화 DB 반영 실패에 따른 전액 보상 취소");
    if (uncertain) {
      assertThat(requests.get(3).method()).isEqualTo("GET");
      assertThat(requests.get(3).path()).isEqualTo("/v1/payments/" + key);
    }
  }

  private PaymentId confirm(int status, String expected) throws Exception {
    var body =
        mvc.perform(
                post("/billings/{id}/payments", billing.getId().value())
                    .with(httpBasic("customer1", "password1"))
                    .contentType("application/json")
                    .content(json.writeValueAsString(Map.of("orderId", order, "paymentKey", key))))
            .andExpect(status().is(status))
            .andExpect(jsonPath("$.status").value(expected))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new PaymentId(json.readTree(body).get("paymentId").asLong());
  }

  private void reconcile(PaymentId id, String expected) throws Exception {
    mvc.perform(
            post("/billings/{id}/payments/{payment}/reconcile", billing.getId().value(), id.value())
                .with(httpBasic("customer1", "password1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(expected));
  }

  private void assertSuccess(PaymentId id) {
    assertThat(payments.load(id).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(billings.load(billing.getId()).orElseThrow().getStatus())
        .isEqualTo(BillingStatus.PAID);
    var subscription = subscriptions.load(billing.getSubscriptionId()).orElseThrow();
    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 3));
  }

  private void assertConfirmation(Request request) {
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.path()).isEqualTo("/v1/payments/confirm");
    assertThat(request.idempotencyKey()).isEqualTo("confirm_" + order);
    assertThat(json.readTree(request.body()))
        .isEqualTo(json.valueToTree(Map.of("paymentKey", key, "orderId", order, "amount", 10000)));
  }

  private String response(boolean cancelled) {
    return json.writeValueAsString(
        Map.of(
            "paymentKey",
            key,
            "orderId",
            order,
            "type",
            "NORMAL",
            "currency",
            "KRW",
            "status",
            cancelled ? "CANCELED" : "DONE",
            "totalAmount",
            10000,
            "balanceAmount",
            cancelled ? 0 : 10000,
            "lastTransactionKey",
            (cancelled ? "cancel_" : "approval_") + order,
            "approvedAt",
            "2026-10-03T10:00:00+09:00",
            "cancels",
            cancelled
                ? List.of(
                    Map.of(
                        "transactionKey",
                        "cancel_" + order,
                        "cancelAmount",
                        10000,
                        "cancelStatus",
                        "DONE",
                        "canceledAt",
                        "2026-10-03T10:00:01+09:00"))
                : List.of()));
  }
}
