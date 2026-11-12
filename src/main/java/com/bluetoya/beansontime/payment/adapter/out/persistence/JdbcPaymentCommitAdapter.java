package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.payment.application.exception.PaymentCommitRetryableException;
import com.bluetoya.beansontime.payment.application.exception.PaymentCommitUncertainException;
import com.bluetoya.beansontime.payment.application.port.out.PaymentCommitPort;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

@Component
@Profile("toss-test & !in-memory")
public class JdbcPaymentCommitAdapter implements PaymentCommitPort {
  public void commit(Runnable action) {
    try {
      action.run();
    } catch (PessimisticLockingFailureException exception) {
      throw new PaymentCommitRetryableException(exception);
    } catch (DataAccessException | TransactionException exception) {
      throw new PaymentCommitUncertainException(exception);
    }
  }
}
