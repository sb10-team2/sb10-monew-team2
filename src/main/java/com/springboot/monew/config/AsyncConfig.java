package com.springboot.monew.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

  @Bean(name = "notificationCreationPool")
  public Executor notificationCreationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setQueueCapacity(20);
    executor.setMaxPoolSize(30);
    executor.setThreadNamePrefix("async-notification-creation-executor-");
    executor.initialize();
    return executor;
  }
}
