package com.springboot.monew.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

@Configuration
public class MongoConfig {

  @Bean(name = "mongoTransactionManager")
  public MongoTransactionManager mongoTransactionManager(
      MongoDatabaseFactory mongoDatabaseFactory
  ) {
    return new MongoTransactionManager(mongoDatabaseFactory);
  }
}
