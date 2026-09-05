package com.bluetoya.beansontime.billing.adapter.in.web;

import com.bluetoya.beansontime.billing.application.exception.BillingAlreadyPaidException;
import com.bluetoya.beansontime.billing.application.exception.BillingNotFoundException;
import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BillingExceptionHandler {

  @ExceptionHandler(BillingNotFoundException.class)
  ProblemDetail handleBillingNotFound(BillingNotFoundException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("청구를 찾을 수 없음");
    problem.setDetail(exception.getMessage());
    return problem;
  }

  @ExceptionHandler(ProductNotFoundException.class)
  ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("상품을 찾을 수 없음");
    problem.setDetail(exception.getMessage());
    return problem;
  }

  @ExceptionHandler(ReactivationBillingNotAllowedException.class)
  ProblemDetail handleReactivationBillingNotAllowed(
      ReactivationBillingNotAllowedException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setTitle("재활성화 청구 준비 불가");
    problem.setDetail(exception.getMessage());
    return problem;
  }

  @ExceptionHandler(BillingAlreadyPaidException.class)
  ProblemDetail handleBillingAlreadyPaid(BillingAlreadyPaidException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setTitle("이미 결제된 청구");
    problem.setDetail(exception.getMessage());
    return problem;
  }
}
