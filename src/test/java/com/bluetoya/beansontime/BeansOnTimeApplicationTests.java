package com.bluetoya.beansontime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class BeansOnTimeApplicationTests {

  @Container @ServiceConnection
  static MySQLContainer mysql =
      new MySQLContainer(DockerImageName.parse("mysql:8.0.39"))
          .withDatabaseName("beans-on-time")
          .withUsername("user")
          .withPassword("password");

  @Test
  void contextLoads() {}
}
