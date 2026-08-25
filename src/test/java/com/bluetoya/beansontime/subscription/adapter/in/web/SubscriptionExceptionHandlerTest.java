package com.bluetoya.beansontime.subscription.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPausePeriodException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionResumeDateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionStateChangeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class SubscriptionExceptionHandlerTest {

  private final SubscriptionExceptionHandler handler = new SubscriptionExceptionHandler();

  @Test
  void mapsInvalidStateChangeToConflict() {
    InvalidSubscriptionStateChangeException exception =
        new InvalidSubscriptionStateChangeException("구독 재개가 불가능합니다.");

    ProblemDetail problem = handler.handleInvalidSubscriptionState(exception);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("구독 상태 변경 불가");
    assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
  }

  @Test
  void mapsInvalidPausePeriodToConflict() {
    InvalidSubscriptionPausePeriodException exception =
        new InvalidSubscriptionPausePeriodException("일시정지 종료일이 유효하지 않습니다.");

    ProblemDetail problem = handler.handleInvalidSubscriptionPausePeriod(exception);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("잘못된 일시정지 기간");
    assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
  }

  @Test
  void mapsInvalidResumeDateToConflict() {
    InvalidSubscriptionResumeDateException exception =
        new InvalidSubscriptionResumeDateException("구독 재개일이 유효하지 않습니다.");

    ProblemDetail problem = handler.handleInvalidSubscriptionResumeDate(exception);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("잘못된 구독 재개일");
    assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
  }
}
