package com.springboot.datagenerator.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadConfig {

  @Bean(name = "jdbcWorker")
  public Executor jdbcWorker() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("jdbc-worker-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "domainExecutorPool")
  public Executor domainExecutorPool() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(6);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("domain-exec-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "dbProducerPool")
  public Executor dbProducerPool() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(20);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("db-producer-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "diskConsumerPool")
  public Executor diskConsumerPool() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(6);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("disk-consumer-");
    executor.initialize();
    return executor;
  }
}
