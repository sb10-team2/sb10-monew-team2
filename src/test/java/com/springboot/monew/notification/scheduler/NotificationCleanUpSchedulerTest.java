package com.springboot.monew.notification.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.springboot.monew.common.metric.ScheduledTaskMetrics;
import com.springboot.monew.notification.metric.NotificationMetrics;
import com.springboot.monew.notification.service.NotificationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanUpSchedulerTest {

  @Mock
  private NotificationService service;

  @Mock
  private ScheduledTaskMetrics scheduledTaskMetrics;

  @Mock
  private NotificationMetrics notificationMetrics;

  private NotificationCleanUpScheduler scheduler;

  @Test
  @DisplayName("system default timezone 기준으로 01:00:00에 실행")
  void purgeOutdatedNotifications() {
    // given
    Clock clock = Clock.fixed(Instant.parse("2026-04-22T01:00:00Z"), ZoneId.systemDefault());
    scheduler = new NotificationCleanUpScheduler(service, clock, scheduledTaskMetrics, notificationMetrics);
    given(service.deleteByChunk(any(Instant.class), any(Long.class))).willReturn(0L);

    // when
    scheduler.purgeOutdatedNotifications();

    // then
    verify(service, times(1)).deleteByChunk(any(Instant.class), any(Long.class));
  }
}
