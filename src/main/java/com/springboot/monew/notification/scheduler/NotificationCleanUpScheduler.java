package com.springboot.monew.notification.scheduler;

import com.springboot.monew.notification.service.NotificationService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCleanUpScheduler {

  private final NotificationService service;
  private final Clock clock;

  @Async("notificationCleanUpPool")
  @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
  public void purgeOutdatedNotifications() {
    long count = 1L;
    long chunkSize = 100;
    Instant threshold = getThresholdDatetime();

    while (count > 0) {
      count = service.deleteByChunk(threshold, chunkSize);
    }
  }

  private Instant getThresholdDatetime() {
    return Instant.now(clock).minus(7, ChronoUnit.DAYS);
  }
}
