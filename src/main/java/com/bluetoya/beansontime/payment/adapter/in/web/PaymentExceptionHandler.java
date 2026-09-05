package com.bluetoya.beansontime.payment.adapter.in.web;

import com.bluetoya.beansontime.payment.application.exception.PaymentAlreadyAttemptedException;
import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

  @ExceptionHandler(PaymentAlreadyAttemptedException.class)
  ProblemDetail handlePaymentAlreadyAttempted(PaymentAlreadyAttemptedException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setTitle("이미 시도한 결제");
    problem.setDetail(exception.getMessage());
    return problem;
  }

  @ExceptionHandler(PaymentGatewayUnavailableException.class)
  ProblemDetail handlePaymentGatewayUnavailable(PaymentGatewayUnavailableException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
    problem.setTitle("결제 대행 시스템 이용 불가");
    problem.setDetail(exception.getMessage());
    return problem;
  }
}
