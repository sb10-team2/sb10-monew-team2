package com.springboot.monew.user.scheduler;

import static org.mockito.Mockito.verify;

import com.springboot.monew.user.outbox.UserActivityOutboxProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityOutboxSchedulerTest {

  @Mock
  private UserActivityOutboxProcessor userActivityOutboxProcessor;

  @InjectMocks
  private UserActivityOutboxScheduler userActivityOutboxScheduler;

  @Test
  @DisplayName("PENDING 상태 UserActivity Outbox 이벤트 처리 스케줄러가 실행되면 Outbox Processor의 미처리 이벤트 처리 메서드를 호출한다")
  void processPendingUserActivityOutboxEvents_callsProcessPendingEvents() {
    // 이 스케줄러 메서드는 별도 입력 계산이나 반환값 사용 없이
    // Processor 메서드를 그대로 위임 호출만 하므로 사전에 stub할 given이 필요 없다.
    // when
    userActivityOutboxScheduler.processPendingUserActivityOutboxEvents();

    // then
    // PENDING 상태 Outbox 이벤트 처리 스케줄러는 Processor의 미처리 이벤트 처리 메서드를 위임 호출해야 한다.
    verify(userActivityOutboxProcessor).processPendingEvents();
  }

  @Test
  @DisplayName("FAILED 상태 UserActivity Outbox 이벤트 재시도 스케줄러가 실행되면 Outbox Processor의 실패 이벤트 복구 메서드를 호출한다")
  void retryFailedUserActivityOutboxEvents_callsRetryFailedEvents() {
    // when
    userActivityOutboxScheduler.retryFailedUserActivityOutboxEvents();

    // then
    // FAILED 상태 Outbox 이벤트 재시도 스케줄러는 Processor의 실패 이벤트 복구 메서드를 위임 호출해야 한다.
    verify(userActivityOutboxProcessor).retryFailedEvents();
  }
}
