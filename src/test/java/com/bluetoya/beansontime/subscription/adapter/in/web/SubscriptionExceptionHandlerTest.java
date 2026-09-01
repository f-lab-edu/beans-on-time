package com.bluetoya.beansontime.subscription.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.subscription.application.exception.ProductNotSubscribableException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPausePeriodException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPeriodStateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionResumeDateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionStateChangeException;
import com.bluetoya.beansontime.subscription.domain.exception.SubscriptionResumeRequiresPaymentException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
  void mapsInvalidPausePeriodToBadRequest() {
    InvalidSubscriptionPausePeriodException exception =
        new InvalidSubscriptionPausePeriodException("일시정지 종료일이 유효하지 않습니다.");

    ProblemDetail problem = handler.handleInvalidSubscriptionPausePeriod(exception);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getTitle()).isEqualTo("잘못된 일시정지 기간");
    assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
  }

  @Test
  void mapsResumeRequiringPaymentToConflict() {
    SubscriptionResumeRequiresPaymentException exception =
        new SubscriptionResumeRequiresPaymentException("재개하려면 새 결제가 필요합니다.");

    ProblemDetail problem = handler.handleResumeRequiresPayment(exception);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("구독 재개에 결제 필요");
    assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
  }

  @Test
  void mapsProductNotSubscribableToConflict() {
    ProductNotSubscribableException exception =
        new ProductNotSubscribableException("현재 상품은 구독할 수 없습니다.");

    ProblemDetail problem = handler.handleProductNotSubscribable(exception);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getTitle()).isEqualTo("신규 구독 불가");
    assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
  }

  @Test
  void doesNotExposeInternalPeriodOrResumeDateInvariantsAsClientErrors() {
    boolean handlesInvalidResumeDate =
        Arrays.stream(SubscriptionExceptionHandler.class.getDeclaredMethods())
            .map(method -> method.getAnnotation(ExceptionHandler.class))
            .filter(annotation -> annotation != null)
            .flatMap(annotation -> Arrays.stream(annotation.value()))
            .anyMatch(InvalidSubscriptionResumeDateException.class::equals);
    boolean handlesInvalidPeriodState =
        Arrays.stream(SubscriptionExceptionHandler.class.getDeclaredMethods())
            .map(method -> method.getAnnotation(ExceptionHandler.class))
            .filter(annotation -> annotation != null)
            .flatMap(annotation -> Arrays.stream(annotation.value()))
            .anyMatch(InvalidSubscriptionPeriodStateException.class::equals);

    assertThat(handlesInvalidResumeDate).isFalse();
    assertThat(handlesInvalidPeriodState).isFalse();
  }
}
