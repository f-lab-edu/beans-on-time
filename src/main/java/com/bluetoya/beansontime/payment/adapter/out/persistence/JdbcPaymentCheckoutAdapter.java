package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.port.out.PaymentCheckoutPort;
import com.bluetoya.beansontime.payment.domain.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("toss-test & !in-memory")
@RequiredArgsConstructor
public class JdbcPaymentCheckoutAdapter implements PaymentCheckoutPort {
  private final JdbcClient jdbc;

  public String prepare(BillingId billingId) {
    String orderId = "bot_" + UUID.randomUUID().toString();
    jdbc.sql("insert into payment_checkouts(order_id, billing_id) values (:order, :billing)")
        .param("order", orderId)
        .param("billing", billingId.value())
        .update();
    return orderId;
  }

  public PaymentAuthorization load(PaymentId paymentId) {
    return jdbc.sql("select order_id, payment_key from payment_checkouts where payment_id = :id")
        .param("id", paymentId.value())
        .query(
            (rs, row) ->
                new PaymentAuthorization(rs.getString("order_id"), rs.getString("payment_key")))
        .single();
  }
}
