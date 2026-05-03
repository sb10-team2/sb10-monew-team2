package com.springboot.monew.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class UserActivityOutboxRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Test
  @DisplayName("PENDING 상태 Outbox 이벤트를 발생 시각 오름차순으로 조회하고 limit만큼만 반환한다")
  void findAllByStatusOrderByOccurredAtAsc_returnsPendingOutboxesInAscendingOrder() {
    // given
    UserActivityOutbox first = saveOutbox(
        UserActivityOutboxStatus.PENDING,
        0,
        Instant.parse("2026-05-01T00:00:00Z")
    );
    UserActivityOutbox second = saveOutbox(
        UserActivityOutboxStatus.PENDING,
        0,
        Instant.parse("2026-05-01T01:00:00Z")
    );
    // limit(2)보다 많은 PENDING 데이터를 준비해, 세 번째 PENDING 이벤트는 결과에서 제외되는지 확인한다.
    saveOutbox(
        UserActivityOutboxStatus.PENDING,
        0,
        Instant.parse("2026-05-01T02:00:00Z")
    );
    // 발생 시각 순서상 중간에 오더라도 상태가 FAILED이면 조회 결과에 포함되지 않는지 확인한다.
    saveOutbox(
        UserActivityOutboxStatus.FAILED,
        1,
        Instant.parse("2026-05-01T00:30:00Z")
    );
    flushAndClear();

    // when
    List<UserActivityOutbox> result =
        userActivityOutboxRepository.findAllByStatusOrderByOccurredAtAsc(
            UserActivityOutboxStatus.PENDING,
            PageRequest.of(0, 2)
        );

    // then
    assertThat(result).hasSize(2);
    assertThat(result).extracting(UserActivityOutbox::getId)
        .containsExactly(first.getId(), second.getId());
    assertThat(result).allMatch(outbox -> outbox.getStatus() == UserActivityOutboxStatus.PENDING);
  }

  @Test
  @DisplayName("FAILED 상태이면서 재시도 횟수가 기준보다 작은 Outbox 이벤트만 발생 시각 오름차순으로 조회한다")
  void findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc_returnsRetryableFailedOutboxes() {
    // given
    UserActivityOutbox retryableFirst = saveOutbox(
        UserActivityOutboxStatus.FAILED,
        1,
        Instant.parse("2026-05-01T00:00:00Z")
    );
    UserActivityOutbox retryableSecond = saveOutbox(
        UserActivityOutboxStatus.FAILED,
        2,
        Instant.parse("2026-05-01T01:00:00Z")
    );
    // retryCount가 기준(3)과 같으면 재시도 대상이 아니므로 조회 결과에서 제외되는지 확인한다.
    saveOutbox(
        UserActivityOutboxStatus.FAILED,
        3,
        Instant.parse("2026-05-01T00:30:00Z")
    );
    // 상태가 PENDING이면 retryCount와 관계없이 FAILED 조회 결과에 포함되지 않는지 확인한다.
    saveOutbox(
        UserActivityOutboxStatus.PENDING,
        0,
        Instant.parse("2026-05-01T00:15:00Z")
    );
    flushAndClear();

    // when
    List<UserActivityOutbox> result =
        userActivityOutboxRepository.findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc(
            UserActivityOutboxStatus.FAILED,
            3,
            PageRequest.of(0, 10)
        );

    // then
    assertThat(result).hasSize(2);
    assertThat(result).extracting(UserActivityOutbox::getId)
        .containsExactly(retryableFirst.getId(), retryableSecond.getId());
    assertThat(result).allMatch(outbox -> outbox.getStatus() == UserActivityOutboxStatus.FAILED);
    assertThat(result).allMatch(outbox -> outbox.getRetryCount() < 3);
  }

  // 조회 조건별 테스트 데이터를 중복 없이 준비하기 위한 헬퍼 메서드다.
  // 상태, 재시도 횟수, 발생 시각만 다르게 조합해 Outbox 엔티티를 저장한다.
  private UserActivityOutbox saveOutbox(
      UserActivityOutboxStatus status,
      int retryCount,
      Instant occurredAt
  ) {
    UserActivityOutbox outbox = new UserActivityOutbox(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        UUID.randomUUID(),
        "{\"event\":\"payload\"}",
        occurredAt
    );

    // FAILED 상태 테스트를 위해 retryCount만큼 실패 처리를 반복해 재시도 횟수를 맞춘다.
    for (int i = 0; i < retryCount; i++) {
      outbox.markFailed("temporary failure");
    }

    // PENDING 상태 테스트에서 retryCount가 있는 데이터를 만들기 위해 상태만 다시 PENDING으로 복구한다.
    if (status == UserActivityOutboxStatus.PENDING && retryCount > 0) {
      outbox.resetToPending();
    }

    // 조회 테스트에 사용할 Outbox 엔티티를 실제 DB에 저장하고 즉시 반영한다.
    em.persist(outbox);
    em.flush();
    return outbox;
  }
}
