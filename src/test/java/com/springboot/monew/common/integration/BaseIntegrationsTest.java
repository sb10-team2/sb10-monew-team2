package com.springboot.monew.common.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

// 모든 통합 테스트의 공통 설정
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationsTest {

  // @Testcontainers + @Container 대신 static 블록으로 수동 기동
  // → 컨테이너가 JVM당 딱 한 번만 시작되어 Spring 컨텍스트 캐싱과 충돌하지 않음
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
  static final MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

  static {
    postgres.start();
    mongo.start();
  }

  // 컨테이너가 기동된 후 실제 할당된 URL/포트를 Spring 컨텍스트에 동적으로 주입
  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    // test.yml의 H2 URL → 컨테이너 실제 PostgreSQL URL로 교체
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    // test.yml의 H2 드라이버 → PostgreSQL 드라이버로 교체
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    // test.yml의 H2Dialect → PostgreSQLDialect로 교체
    registry.add("spring.jpa.properties.hibernate.dialect",
        () -> "org.hibernate.dialect.PostgreSQLDialect");
    // PostgreSQL은 비임베디드 DB라 기본값이 never → always로 강제해서 schema.sql 실행
    registry.add("spring.sql.init.mode", () -> "always");
    // schema.sql로 테이블 생성 후 Hibernate가 엔티티와 스키마 일치 여부만 검증
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    // test.yml의 고정 MongoDB URI → 컨테이너 실제 URI로 교체
    registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
  }
}
