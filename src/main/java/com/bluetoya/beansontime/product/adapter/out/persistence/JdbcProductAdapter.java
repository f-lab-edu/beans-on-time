package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.*;
import com.bluetoya.beansontime.seller.domain.SellerId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcProductAdapter implements SaveProductPort, LoadProductPort {
  private final JdbcClient jdbc;

  @Override
  public void saveNew(Product product) {
    jdbc.sql(
            """
        insert into products (id, seller_id, name, description, base_price, supply_status)
        values (:id, :sellerId, :name, :description, :price, :status)
        """)
        .param("id", product.getId().id())
        .param("sellerId", product.getSellerId().id())
        .param("name", product.getName())
        .param("description", product.getDescription())
        .param("price", product.getBasePrice().price())
        .param("status", product.getSupplyStatus().name())
        .update();
  }

  @Override
  public void save(Product product) {
    int updated =
        jdbc.sql("update products set supply_status = :status where id = :id")
            .param("status", product.getSupplyStatus().name())
            .param("id", product.getId().id())
            .update();
    if (updated != 1) throw new ProductNotFoundException("저장할 상품이 존재하지 않습니다.");
  }

  @Override
  public Optional<Product> load(ProductId id) {
    return jdbc.sql("select * from products where id = :id")
        .param("id", id.id())
        .query(
            (rs, row) ->
                Product.restore(
                    new ProductId(rs.getLong("id")),
                    new SellerId(rs.getLong("seller_id")),
                    rs.getString("name"),
                    rs.getString("description"),
                    new Money(rs.getInt("base_price")),
                    SupplyStatus.valueOf(rs.getString("supply_status"))))
        .optional();
  }
}
