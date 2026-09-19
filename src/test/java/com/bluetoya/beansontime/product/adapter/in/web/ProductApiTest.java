package com.bluetoya.beansontime.product.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.product.adapter.out.persistence.*;
import com.bluetoya.beansontime.product.adapter.out.security.CurrentSellerSecurityAdapter;
import com.bluetoya.beansontime.product.application.service.GetProductDetailService;
import com.bluetoya.beansontime.product.application.service.RegisterProductService;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.security.adapter.springsecurity.SecurityCurrentActorProvider;
import com.bluetoya.beansontime.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductController.class)
@Import({
  ProductApiTest.SecurityTestConfig.class,
  SecurityConfig.class,
  RegisterProductService.class,
  GetProductDetailService.class,
  InMemoryProductRepository.class,
  InMemoryProductAdapter.class,
  InMemoryGetProductDetailQueryAdapter.class,
  CurrentSellerSecurityAdapter.class,
  SecurityCurrentActorProvider.class,
  ProductExceptionHandler.class
})
class ProductApiTest {
  @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
  @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
  static class SecurityTestConfig {}

  @Autowired MockMvc mockMvc;
  @Autowired InMemoryProductRepository repository;

  @Test
  void registersWithSellerIdentityAndExposesPublicDetail() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/products")
                    .with(httpBasic("seller1", "password1"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"원두\",\"basePrice\":5000,\"sellerId\":2}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = new ObjectMapper().readTree(body).get("productId").asLong();
    assertThat(repository.findById(new ProductId(id)).orElseThrow().sellerId().id())
        .isEqualTo(1);
    mockMvc
        .perform(get("/products/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(id))
        .andExpect(jsonPath("$.name").value("원두"))
        .andExpect(jsonPath("$.description").value(""))
        .andExpect(jsonPath("$.basePrice").value(5000))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"name\":\"원두\",\"basePrice\":-1}",
        "{\"name\":\"원두\",\"basePrice\":null}",
        "{\"name\":\"원두\"}"
      })
  void rejectsInvalidPrices(String body) throws Exception {
    mockMvc
        .perform(
            post("/products")
                .with(httpBasic("seller1", "password1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void acceptsZeroPrice() throws Exception {
    mockMvc
        .perform(
            post("/products")
                .with(httpBasic("seller1", "password1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"원두\",\"basePrice\":0}"))
        .andExpect(status().isCreated());
  }

  @Test
  void distinguishesMissingProductsFromInvalidIds() throws Exception {
    mockMvc
        .perform(get("/products/9223372036854775807"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("상품을 찾을 수 없음"));
    mockMvc.perform(get("/products/0")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/products/-1")).andExpect(status().isBadRequest());
  }

  @Test
  void requiresSellerRoleForRegistration() throws Exception {
    String body = "{\"name\":\"원두\",\"basePrice\":5000}";
    mockMvc
        .perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/products")
                .with(httpBasic("customer1", "password1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }
}
