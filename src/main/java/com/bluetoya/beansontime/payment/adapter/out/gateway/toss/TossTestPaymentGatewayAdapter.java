package com.bluetoya.beansontime.payment.adapter.out.gateway.toss;

import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.domain.*;
import java.time.*;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TossTestPaymentGatewayAdapter
    implements PaymentGateway,
        FindGatewayPaymentPort,
        CancelGatewayPaymentPort,
        com.bluetoya.beansontime.refund.application.port.out.RefundGateway {
  private final TossTestPaymentClient client;
  private final PaymentCheckoutPort checkouts;
  private final LoadPaymentPort payments;
  private final Clock clock;

  public PaymentGatewayResult pay(PaymentGatewayRequest request) {
    var payment = payments.load(request.paymentId()).orElseThrow();
    var auth = checkouts.load(request.paymentId());
    if (!payment.getBillingId().equals(request.billingId())
        || !payment.getAmount().equals(request.amount()))
      throw new IllegalArgumentException("저장한 결제 요청과 다릅니다.");
    var response =
        call(
            () ->
                client.confirm(
                    new TossTestPaymentClient.ConfirmationRequest(
                        auth.paymentKey(), auth.orderId(), payment.getAmount().price()),
                    "confirm_" + auth.orderId()));
    return result(payment, auth, response).orElseThrow(() -> unavailable());
  }

  public Optional<PaymentGatewayResult> find(PaymentId id) {
    var payment = payments.load(id).orElseThrow();
    var auth = checkouts.load(id);
    return result(payment, auth, call(() -> client.find(auth.paymentKey())));
  }

  private Optional<PaymentGatewayResult> result(
      Payment payment, PaymentAuthorization auth, TossTestPaymentClient.PaymentResponse response) {
    validate(payment, auth, response);
    if ("DONE".equals(response.status())
        && response.approvedAt() != null
        && valid(response.lastTransactionKey())
        && response.balanceAmount() != null
        && response.balanceAmount() == payment.getAmount().price()
        && response.cancels().isEmpty()) {
      return Optional.of(
          PaymentGatewayResult.approved(response.lastTransactionKey(), kst(response.approvedAt())));
    }
    if (("ABORTED".equals(response.status()) || "EXPIRED".equals(response.status()))
        && response.approvedAt() == null
        && response.cancels().isEmpty())
      return Optional.of(PaymentGatewayResult.declined(LocalDateTime.now(clock)));
    return Optional.empty();
  }

  public Optional<CancellationReceipt> cancelOrFind(Payment payment, PaymentApproval approval) {
    var auth = checkouts.load(payment.getId());
    var response = call(() -> client.find(auth.paymentKey()));
    validate(payment, auth, response);
    var found = cancellation(payment, approval, response);
    if (found.isPresent()) return found;
    if (response.balanceAmount() == null
        || response.balanceAmount() != payment.getAmount().price()
        || !"DONE".equals(response.status())
        || !approval.transactionId().equals(response.lastTransactionKey())
        || response.approvedAt() == null
        || !approval.approvedAt().equals(kst(response.approvedAt()))
        || !response.cancels().isEmpty()) return Optional.empty();
    // 유효기간 이후에는 새 멱등키나 새 취소 요청을 만들지 않고 조회·운영 확인만 한다.
    if (!LocalDateTime.now(clock).isBefore(approval.cancelRequestedAt().plusDays(15)))
      return Optional.empty();
    var cancelled =
        call(
            () ->
                client.cancel(
                    auth.paymentKey(), "구독 활성화 DB 반영 실패에 따른 전액 보상 취소", approval.cancelKey()));
    validate(payment, auth, cancelled);
    return cancellation(payment, approval, cancelled);
  }

  private Optional<CancellationReceipt> cancellation(
      Payment payment, PaymentApproval approval, TossTestPaymentClient.PaymentResponse response) {
    if (!"CANCELED".equals(response.status())
        || response.balanceAmount() == null
        || response.balanceAmount() != 0
        || response.approvedAt() == null
        || !approval.approvedAt().equals(kst(response.approvedAt()))
        || response.cancels().size() != 1) return Optional.empty();
    var cancel = response.cancels().getFirst();
    if (!"DONE".equals(cancel.cancelStatus())
        || cancel.cancelAmount() == null
        || cancel.cancelAmount() != payment.getAmount().price()
        || !valid(cancel.transactionKey())
        || cancel.canceledAt() == null
        || !cancel.transactionKey().equals(response.lastTransactionKey())) return Optional.empty();
    return Optional.of(new CancellationReceipt(cancel.transactionKey(), kst(cancel.canceledAt())));
  }

  public Optional<com.bluetoya.beansontime.refund.application.port.out.RefundGateway.Receipt>
      refundOrFind(com.bluetoya.beansontime.refund.domain.Refund refund) {
    var payment = payments.load(refund.paymentId()).orElseThrow();
    var auth = checkouts.load(refund.paymentId());
    var response = call(() -> client.find(auth.paymentKey()));
    validate(payment, auth, response);
    var found = refundReceipt(refund, response);
    if (found.isPresent()) return found;
    if (!"DONE".equals(response.status())
        || !response.cancels().isEmpty()
        || response.balanceAmount() == null
        || response.balanceAmount() != refund.amount().price()
        || !refund.approvalTransactionId().equals(response.lastTransactionKey())
        || response.approvedAt() == null
        || !refund.approvedAt().equals(kst(response.approvedAt()))) return Optional.empty();
    // 기존 보상 취소와 동일하게 멱등키 보장 기간 이후에는 조회만 수행한다.
    if (!LocalDateTime.now(clock).isBefore(refund.requestedAt().plusDays(15)))
      return Optional.empty();
    var cancelled =
        call(() -> client.cancel(auth.paymentKey(), "구독 철회에 따른 전액 환불", refund.idempotencyKey()));
    validate(payment, auth, cancelled);
    return refundReceipt(refund, cancelled);
  }

  private Optional<com.bluetoya.beansontime.refund.application.port.out.RefundGateway.Receipt>
      refundReceipt(
          com.bluetoya.beansontime.refund.domain.Refund refund,
          TossTestPaymentClient.PaymentResponse response) {
    if (!"CANCELED".equals(response.status())
        || response.balanceAmount() == null
        || response.balanceAmount() != 0
        || response.approvedAt() == null
        || !refund.approvedAt().equals(kst(response.approvedAt()))
        || response.cancels().size() != 1) return Optional.empty();
    var cancel = response.cancels().getFirst();
    if (!"DONE".equals(cancel.cancelStatus())
        || cancel.cancelAmount() == null
        || cancel.cancelAmount() != refund.amount().price()
        || !valid(cancel.transactionKey())
        || cancel.canceledAt() == null
        || !cancel.transactionKey().equals(response.lastTransactionKey())) return Optional.empty();
    return Optional.of(
        new com.bluetoya.beansontime.refund.application.port.out.RefundGateway.Receipt(
            cancel.transactionKey(), kst(cancel.canceledAt())));
  }

  private void validate(
      Payment payment, PaymentAuthorization auth, TossTestPaymentClient.PaymentResponse response) {
    if (!auth.paymentKey().equals(response.paymentKey())
        || !auth.orderId().equals(response.orderId())
        || !"NORMAL".equals(response.type())
        || !"KRW".equals(response.currency())
        || response.totalAmount() == null
        || response.totalAmount() != payment.getAmount().price()) throw unavailable();
  }

  private boolean valid(String value) {
    return value != null && !value.isBlank();
  }

  private LocalDateTime kst(OffsetDateTime at) {
    return at.atZoneSameInstant(ZoneId.of("Asia/Seoul"))
        .toLocalDateTime()
        .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  }

  private TossTestPaymentClient.PaymentResponse call(
      Supplier<TossTestPaymentClient.PaymentResponse> call) {
    try {
      return call.get();
    } catch (TossTestApiException exception) {
      throw new PaymentGatewayUnavailableException("토스 테스트 API 결과 확인 필요: " + exception.code());
    }
  }

  private PaymentGatewayUnavailableException unavailable() {
    return new PaymentGatewayUnavailableException("토스 테스트 결제 결과를 확인 중입니다.");
  }
}
