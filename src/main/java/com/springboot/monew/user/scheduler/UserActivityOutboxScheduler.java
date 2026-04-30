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
    log.debug("사용자 활동 Outbox 스케줄러 실행");
    userActivityOutboxProcessor.processPendingEvents();
  }
}
