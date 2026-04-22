package com.springboot.monew.config;

import java.time.Clock;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class SchedulingConfig {

  @Bean(name = "notificationCleanUpPool")
  public Executor notificationCleanUpExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setQueueCapacity(5);
    executor.setMaxPoolSize(10);
    executor.setThreadNamePrefix("async-notification-cleanup-executor-");
    executor.initialize();
    return executor;
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
