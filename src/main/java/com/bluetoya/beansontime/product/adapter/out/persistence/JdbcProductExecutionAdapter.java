package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.out.ProductExecutionPort;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcProductExecutionAdapter implements ProductExecutionPort {
  private final JdbcClient jdbc;
  private final TransactionTemplate transactions;

  public <T> T execute(ProductId id, Supplier<T> action) {
    return transactions.execute(
        status -> {
          // 행이 없을 때의 도메인 예외와 소유권 인가는 유즈케이스에서 판단한다.
          jdbc.sql("select id from products where id = :id for update")
              .param("id", id.id())
              .query(Long.class)
              .optional();
          return action.get();
        });
  }
}
