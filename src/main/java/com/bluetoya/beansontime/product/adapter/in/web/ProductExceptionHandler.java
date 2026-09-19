package com.bluetoya.beansontime.product.adapter.in.web;

import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductExceptionHandler {
  @ExceptionHandler(ProductNotFoundException.class)
  ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("상품을 찾을 수 없음");
    problem.setDetail(exception.getMessage());
    return problem;
  }
}
