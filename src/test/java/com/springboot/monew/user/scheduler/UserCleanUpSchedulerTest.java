package com.springboot.monew.user.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.springboot.monew.common.metric.ScheduledTaskMetrics;
import com.springboot.monew.user.metric.UserMetrics;
import com.springboot.monew.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCleanUpSchedulerTest {

  @Mock
  private UserService userService;

  @Mock
  private ScheduledTaskMetrics scheduledTaskMetrics;

  @Mock
  private UserMetrics userMetrics;

  @Test
  @DisplayName("삭제 후 24시간이 지난 사용자를 정리한다")
  void purgeDeletedUsers() {
    // given
    // 스케줄러 내부에서 Instant.now(clock)로 현재 시각을 기준으로 cutoff를 계산하므로,
    // 실행 시점에 따라 결과가 달라지지 않도록 고정된 Clock을 사용한다.
    Clock clock = Clock.fixed(Instant.parse("2026-05-03T01:00:00Z"), ZoneId.systemDefault());
    UserCleanUpScheduler scheduler =
        new UserCleanUpScheduler(userService, scheduledTaskMetrics, clock, userMetrics);

    Instant expectedCutoff = Instant.parse("2026-05-02T01:00:00Z");

    // 삭제 대상 사용자 수를 미리 준비한다.
    given(userService.purgeDeletedUsersOlderThan(expectedCutoff)).willReturn(3);

    // when
    scheduler.purgeDeletedUsers();

    // then
    // 현재 시각 기준 24시간 이전 cutoff로 삭제 대상 사용자를 정리해야 한다.
    verify(userService).purgeDeletedUsersOlderThan(expectedCutoff);
  }

  @Test
  @DisplayName("사용자 정리 중 예외가 발생하면 예외를 다시 던진다")
  void purgeDeletedUsers_throwsException_whenCleanupFails() {
    // given
    Clock clock = Clock.fixed(Instant.parse("2026-05-03T01:00:00Z"), ZoneId.systemDefault());
    UserCleanUpScheduler scheduler =
        new UserCleanUpScheduler(userService, scheduledTaskMetrics, clock, userMetrics);

    Instant expectedCutoff = Instant.parse("2026-05-02T01:00:00Z");

    // 사용자 정리 수행 중 예외가 발생한 상황을 가정한다.
    given(userService.purgeDeletedUsersOlderThan(expectedCutoff))
        .willThrow(new IllegalStateException("cleanup failed"));

    // when & then
    // 정리 중 예외가 발생하면 예외를 삼키지 않고 호출자에게 다시 전달해야 한다.
    assertThatThrownBy(scheduler::purgeDeletedUsers)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("cleanup failed");

    // 예외가 발생해도 삭제 대상 사용자 정리 시도는 수행되어야 한다.
    verify(userService).purgeDeletedUsersOlderThan(expectedCutoff);
  }
}
