package com.springboot.monew.notification.scheduler;

import com.springboot.monew.metric.MonewTaskNames;
import com.springboot.monew.metric.ScheduledTaskMetrics;
import com.springboot.monew.metric.notification.NotificationMetrics;
import com.springboot.monew.notification.service.NotificationService;
import java.time.Clock;
import java.time.Duration;
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
  private final ScheduledTaskMetrics scheduledTaskMetrics;
  private final NotificationMetrics notificationMetrics;

  @Async("notificationCleanUpPool")
  @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
  public void purgeOutdatedNotifications() {
    Instant startedAt = Instant.now(clock);
    long count = 1L;
    long chunkSize = 100;
    long deletedCount = 0L;
    long chunkCount = 0L;
    Instant threshold = getThresholdDatetime();

    try {
      while (count > 0) {
        count = service.deleteByChunk(threshold, chunkSize);
        deletedCount += count;
        if (count > 0) {
          chunkCount++;
        }
      }

      notificationMetrics.recordCleanupSuccess(deletedCount, chunkCount);
      scheduledTaskMetrics.recordSuccess(MonewTaskNames.NOTIFICATION_CLEANUP,
          Duration.between(startedAt, Instant.now(clock)));
    } catch (Exception e) {
      scheduledTaskMetrics.recordFailure(MonewTaskNames.NOTIFICATION_CLEANUP,
          Duration.between(startedAt, Instant.now(clock)));
      throw e;
    }
  }

  private Instant getThresholdDatetime() {
    return Instant.now(clock).minus(7, ChronoUnit.DAYS);
  }
}
