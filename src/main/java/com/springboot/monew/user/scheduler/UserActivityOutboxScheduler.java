package com.springboot.monew.user.scheduler;

import com.springboot.monew.user.outbox.UserActivityOutboxProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityOutboxScheduler {

  private final UserActivityOutboxProcessor userActivityOutboxProcessor;

  // 미처리 사용자 활동 Outbox 이벤트를 주기적으로 처리한다.
  // 이전 실행이 끝난 뒤 30초 후 다시 실행, 애플리케이션 시작 10초 뒤 첫 실행.
  @Scheduled(fixedDelay = 30000, initialDelay = 10000)
  public void processPendingUserActivityOutboxEvents() {
    log.debug("사용자 활동 Outbox PENDING 처리 스케줄러 실행");
    userActivityOutboxProcessor.processPendingEvents();
  }

  // 처리 실패한 이벤트 중 재시도 가능한 건을 다시 PENDING 으로 복원한다.
  @Scheduled(fixedDelay = 30000, initialDelay = 15000)
  public void retryFailedUserActivityOutboxEvents() {
    log.debug("사용자 활동 Outbox FAILED 재시도 스케줄러 실행");
    userActivityOutboxProcessor.retryFailedEvents();
  }
}
