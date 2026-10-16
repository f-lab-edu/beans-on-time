package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.in.ProductDetail;
import com.bluetoya.beansontime.product.application.port.out.GetProductDetailQueryPort;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcGetProductDetailQueryAdapter implements GetProductDetailQueryPort {
  private final JdbcClient jdbc;

  @Override
  public Optional<ProductDetail> find(ProductId id) {
    return jdbc.sql(
            "select id, name, description, base_price, supply_status from products where id = :id")
        .param("id", id.id())
        .query(
            (rs, row) ->
                new ProductDetail(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("base_price"),
                    rs.getString("supply_status")))
        .optional();
  }
}
