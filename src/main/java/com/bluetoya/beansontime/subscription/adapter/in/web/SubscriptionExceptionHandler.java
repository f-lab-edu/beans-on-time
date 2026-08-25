package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.subscription.application.exception.DuplicateSubscriptionException;
import com.bluetoya.beansontime.subscription.application.exception.InvalidSubscriptionStateChangeException;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SubscriptionExceptionHandler {
  @ExceptionHandler(SubscriptionNotFoundException.class)
  ProblemDetail handleSubscriptionNotFound(SubscriptionNotFoundException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

    problem.setTitle("구독을 찾을 수 없음");
    problem.setDetail(exception.getMessage());

    return problem;
  }

  @ExceptionHandler(DuplicateSubscriptionException.class)
  ProblemDetail handleDuplicateSubscription(DuplicateSubscriptionException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);

    problem.setTitle("중복 구독");
    problem.setDetail(exception.getMessage());

    return problem;
  }

  @ExceptionHandler(InvalidSubscriptionStateChangeException.class)
  ProblemDetail handleInvalidSubscriptionState(InvalidSubscriptionStateChangeException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);

    problem.setTitle("구독 상태 변경 불가");
    problem.setDetail(exception.getMessage());

    return problem;
  }
}
