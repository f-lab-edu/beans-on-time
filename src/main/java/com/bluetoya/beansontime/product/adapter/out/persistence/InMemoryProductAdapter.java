package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class InMemoryProductAdapter implements SaveProductPort {
    private final InMemoryProductRepository productRepository;

    @Override
    public void save(Product product) {
        productRepository.save(product);
    }
}
