package com.bluetoya.beansontime.payment.adapter.out.gateway;

import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGateway;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayRequest;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGatewayAdapter implements PaymentGateway {
  private Result nextResult = Result.APPROVE;
  private String nextTransactionId;

  @Override
  public synchronized PaymentGatewayResult pay(PaymentGatewayRequest request) {
    Result result = nextResult;
    String transactionId = nextTransactionId;
    nextResult = Result.APPROVE;
    nextTransactionId = null;

    return switch (result) {
      case APPROVE ->
          PaymentGatewayResult.approved(
              transactionId == null ? UUID.randomUUID().toString() : transactionId);
      case DECLINE -> PaymentGatewayResult.declined();
      case UNAVAILABLE -> throw new PaymentGatewayUnavailableException("결제 대행 시스템에 연결할 수 없습니다.");
    };
  }

  public synchronized void approveNext(String transactionId) {
    this.nextResult = Result.APPROVE;
    this.nextTransactionId = transactionId;
  }

  public synchronized void declineNext() {
    this.nextResult = Result.DECLINE;
    this.nextTransactionId = null;
  }

  public synchronized void failNext() {
    this.nextResult = Result.UNAVAILABLE;
    this.nextTransactionId = null;
  }

  private enum Result {
    APPROVE,
    DECLINE,
    UNAVAILABLE
  }
}
