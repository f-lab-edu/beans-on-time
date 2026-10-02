package com.bluetoya.beansontime.payment.adapter.in.web;

import com.bluetoya.beansontime.payment.application.exception.PaymentInProgressException;
import com.bluetoya.beansontime.payment.application.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

  @ExceptionHandler(PaymentInProgressException.class)
  ProblemDetail handlePaymentInProgress(PaymentInProgressException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setTitle("결제 결과 확인 중");
    problem.setProperty("paymentId", exception.getPaymentId().value());
    problem.setDetail(exception.getMessage());
    return problem;
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  ProblemDetail handlePaymentNotFound(PaymentNotFoundException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("결제 시도를 찾을 수 없음");
    problem.setDetail(exception.getMessage());
    return problem;
  }
}
