package com.springboot.monew.users.scheduler;

import com.springboot.monew.users.service.UserService;
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

  // 매 정각마다 스케줄러가 실행된다. (1시간 마다 실행됨.)
  @Scheduled(cron = "0 0 * * * *")
  public void purgeDeletedUsers() {
    // 지금 기준으로 24시간 전 시각을 계산해서 cutoff로 만든다.
    Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

    int deletedCount = userService.purgeDeletedUsersOlderThan(cutoff);

    log.info("사용자 자동 물리 삭제 스케줄 완료 - deletedCount={}", deletedCount);
  }
}
