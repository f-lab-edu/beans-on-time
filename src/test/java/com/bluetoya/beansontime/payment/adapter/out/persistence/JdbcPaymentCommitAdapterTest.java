package com.bluetoya.beansontime.payment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.payment.application.exception.PaymentCommitRetryableException;
import com.bluetoya.beansontime.payment.application.exception.PaymentCommitUncertainException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;

class JdbcPaymentCommitAdapterTest {
  private final JdbcPaymentCommitAdapter adapter = new JdbcPaymentCommitAdapter();

  @Test
  void onlyLockDeadlockAndSerializationFailuresAreRetryable() {
    for (var failure :
        List.of(
            new CannotAcquireLockException("잠금 실패"),
            new DeadlockLoserDataAccessException("데드락", null),
            new CannotSerializeTransactionException("직렬화 실패"))) {
      assertThatThrownBy(
              () ->
                  adapter.commit(
                      () -> {
                        throw failure;
                      }))
          .isInstanceOf(PaymentCommitRetryableException.class)
          .hasCause(failure);
    }
    var resourceFailure = new TransientDataAccessResourceException("연결 실패");
    assertThatThrownBy(
            () ->
                adapter.commit(
                    () -> {
                      throw resourceFailure;
                    }))
        .isExactlyInstanceOf(PaymentCommitUncertainException.class)
        .hasCause(resourceFailure);
  }
}
