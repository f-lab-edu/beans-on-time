package com.bluetoya.beansontime.payment.adapter.out.gateway;

import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.port.out.FindGatewayPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGateway;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayRequest;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FakePaymentGatewayAdapter implements PaymentGateway, FindGatewayPaymentPort {
  private final Clock clock;
  private final Map<PaymentId, PaymentGatewayRequest> requests = new HashMap<>();
  private final Map<PaymentId, PaymentGatewayResult> results = new HashMap<>();
  private Result nextResult = Result.APPROVE;
  private String nextTransactionId;

  @Override
  public synchronized PaymentGatewayResult pay(PaymentGatewayRequest request) {
    var existing = requests.putIfAbsent(request.paymentId(), request);
    if (existing != null) {
      if (!existing.equals(request)) {
        throw new IllegalArgumentException("같은 결제 시도의 요청 내용은 변경할 수 없습니다.");
      }
      return find(request.paymentId())
          .orElseThrow(() -> new PaymentGatewayUnavailableException("기존 결제 결과를 확인 중입니다."));
    }
    Result result = nextResult;
    String transactionId = nextTransactionId;
    nextResult = Result.APPROVE;
    nextTransactionId = null;
    PaymentGatewayResult receipt =
        switch (result) {
          case APPROVE ->
              PaymentGatewayResult.approved(
                  transactionId == null ? UUID.randomUUID().toString() : transactionId,
                  LocalDateTime.now(clock));
          case DECLINE -> PaymentGatewayResult.declined(LocalDateTime.now(clock));
          case UNAVAILABLE ->
              throw new PaymentGatewayUnavailableException("결제 대행 시스템에 연결할 수 없습니다.");
        };
    results.put(request.paymentId(), receipt);
    return receipt;
  }

  @Override
  public synchronized Optional<PaymentGatewayResult> find(PaymentId paymentId) {
    return Optional.ofNullable(results.get(paymentId));
  }

  public synchronized void approvePending(PaymentId paymentId, String transactionId) {
    resolve(paymentId, PaymentGatewayResult.approved(transactionId, LocalDateTime.now(clock)));
  }

  public synchronized void declinePending(PaymentId paymentId) {
    resolve(paymentId, PaymentGatewayResult.declined(LocalDateTime.now(clock)));
  }

  private void resolve(PaymentId paymentId, PaymentGatewayResult result) {
    if (!requests.containsKey(paymentId) || results.containsKey(paymentId)) {
      throw new IllegalStateException("결과가 미확정인 결제만 확정할 수 있습니다.");
    }
    results.put(paymentId, result);
  }

  public synchronized void approveNext(String transactionId) {
    nextResult = Result.APPROVE;
    nextTransactionId = transactionId;
  }

  public synchronized void declineNext() {
    nextResult = Result.DECLINE;
    nextTransactionId = null;
  }

  public synchronized void failNext() {
    nextResult = Result.UNAVAILABLE;
    nextTransactionId = null;
  }

  private enum Result {
    APPROVE,
    DECLINE,
    UNAVAILABLE
  }
}
