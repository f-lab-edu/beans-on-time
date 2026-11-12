package com.bluetoya.beansontime.refund.domain;

import static org.assertj.core.api.Assertions.*;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.product.domain.Money;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefundTest {
  @Test
  void windowIncludesExactly24HoursButNotFutureApprovalsOrMissingEvidence() {
    var approved = LocalDateTime.of(2026, 10, 8, 10, 0);
    assertThat(Refund.withinWindow(approved, approved)).isTrue();
    assertThat(Refund.withinWindow(approved, approved.plusHours(24))).isTrue();
    assertThat(Refund.withinWindow(approved, approved.plusHours(24).plusNanos(1000))).isFalse();
    assertThat(Refund.withinWindow(approved, approved.minusSeconds(1))).isFalse();
    assertThat(Refund.withinWindow(null, approved)).isFalse();
  }

  @Test
  void completionPreservesRequestAndRejectsConflictingReceipts() {
    var at = LocalDateTime.of(2026, 10, 8, 10, 0);
    var refund = Refund.request(new PaymentId(1), new Money(10000), "approval", at, at);
    var completed = refund.complete("refund", at.plusMinutes(1));
    assertThat(completed.isCompleted()).isTrue();
    assertThat(completed.idempotencyKey()).isEqualTo(refund.idempotencyKey());
    assertThat(completed.complete("refund", at.plusMinutes(1))).isEqualTo(completed);
    assertThatThrownBy(() -> completed.complete("different", at.plusMinutes(1)))
        .isInstanceOf(IllegalStateException.class);
  }
}
