package com.springboot.monew.user.scheduler;

import com.springboot.monew.common.metric.MonewTaskNames;
import com.springboot.monew.common.metric.ScheduledTaskMetrics;
import com.springboot.monew.user.metric.UserMetrics;
import com.springboot.monew.user.service.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserCleanUpScheduler {

  private final UserService userService;
  private final ScheduledTaskMetrics scheduledTaskMetrics;
  private final Clock clock;
  private final UserMetrics userMetrics;

  // 매 정각마다 스케줄러가 실행된다. (1시간 마다 실행됨.)
  @Scheduled(cron = "0 0 * * * *")
  public void purgeDeletedUsers() {
    Instant startedAt = Instant.now(clock);

    // 지금 기준으로 24시간 전 시각을 계산해서 cutoff로 만든다.
    Instant cutoff = startedAt.minus(24, ChronoUnit.HOURS);

    try {
      int deletedCount = userService.purgeDeletedUsersOlderThan(cutoff);

      userMetrics.recordCleanupSuccess(deletedCount);
      scheduledTaskMetrics.recordSuccess(MonewTaskNames.USER_CLEANUP,
          Duration.between(startedAt, Instant.now(clock)));

      log.info("사용자 자동 물리 삭제 스케줄 완료 - deletedCount={}", deletedCount);
    } catch (Exception e) {
      scheduledTaskMetrics.recordFailure(MonewTaskNames.USER_CLEANUP,
          Duration.between(startedAt, Instant.now(clock)));
      throw e;
    }
  }
}
