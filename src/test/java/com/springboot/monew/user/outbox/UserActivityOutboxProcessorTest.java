package com.springboot.monew.user.outbox;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserActivityOutboxProcessorTest {

  @Mock
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Mock
  private UserActivityOutboxSingleProcessor userActivityOutboxSingleProcessor;

  @InjectMocks
  private UserActivityOutboxProcessor userActivityOutboxProcessor;

  @Test
  @DisplayName("PENDING 상태 Outbox 이벤트를 발생 시각 순으로 조회해 각각 처리한다")
  void processPendingEvents_processesAllPendingOutboxes() {
    // given
    UUID firstOutboxId = UUID.randomUUID();
    UUID secondOutboxId = UUID.randomUUID();

    UserActivityOutbox firstOutbox = mock(UserActivityOutbox.class);
    UserActivityOutbox secondOutbox = mock(UserActivityOutbox.class);

    // 조회된 Outbox 이벤트마다 개별 처리에 사용할 id를 미리 준비한다.
    given(firstOutbox.getId()).willReturn(firstOutboxId);
    given(secondOutbox.getId()).willReturn(secondOutboxId);

    // PENDING 상태 이벤트를 100건 제한의 첫 페이지로 조회하도록 기대한다.
    given(userActivityOutboxRepository.findAllByStatusOrderByOccurredAtAsc(
        UserActivityOutboxStatus.PENDING,
        PageRequest.of(0, 100)
    )).willReturn(List.of(firstOutbox, secondOutbox));

    // when
    userActivityOutboxProcessor.processPendingEvents();

    // then
    // PENDING 상태 이벤트 조회가 지정된 조건으로 수행되었는지 검증한다.
    verify(userActivityOutboxRepository).findAllByStatusOrderByOccurredAtAsc(
        UserActivityOutboxStatus.PENDING,
        PageRequest.of(0, 100)
    );

    // 조회된 각 Outbox 이벤트가 id 기준으로 개별 처리되어야 한다.
    verify(userActivityOutboxSingleProcessor).processSingleEvent(firstOutboxId);
    verify(userActivityOutboxSingleProcessor).processSingleEvent(secondOutboxId);
  }

  @Test
  @DisplayName("PENDING 상태 Outbox 이벤트 중 하나의 처리에 실패해도 다음 이벤트 처리는 계속한다")
  void processPendingEvents_continuesWhenSingleProcessingFails() {
    // given
    UUID firstOutboxId = UUID.randomUUID();
    UUID secondOutboxId = UUID.randomUUID();

    UserActivityOutbox firstOutbox = mock(UserActivityOutbox.class);
    UserActivityOutbox secondOutbox = mock(UserActivityOutbox.class);

    given(firstOutbox.getId()).willReturn(firstOutboxId);
    given(secondOutbox.getId()).willReturn(secondOutboxId);

    given(userActivityOutboxRepository.findAllByStatusOrderByOccurredAtAsc(
        UserActivityOutboxStatus.PENDING,
        PageRequest.of(0, 100)
    )).willReturn(List.of(firstOutbox, secondOutbox));

    // 첫 번째 이벤트 처리에서 예외가 발생하더라도 다음 이벤트 처리는 계속되어야 한다.
    willThrow(new IllegalStateException("single processing failed"))
        .given(userActivityOutboxSingleProcessor)
        .processSingleEvent(firstOutboxId);

    // when
    userActivityOutboxProcessor.processPendingEvents();

    // then
    // 첫 번째 이벤트 처리 실패가 발생했더라도 두 번째 이벤트 처리까지 시도해야 한다.
    verify(userActivityOutboxSingleProcessor).processSingleEvent(firstOutboxId);
    verify(userActivityOutboxSingleProcessor).processSingleEvent(secondOutboxId);
  }

  @Test
  @DisplayName("FAILED 상태이면서 재시도 횟수가 제한보다 작은 Outbox 이벤트를 조회해 각각 PENDING 상태로 복구한다")
  void retryFailedEvents_resetsRetryableFailedOutboxes() {
    // given
    UUID firstOutboxId = UUID.randomUUID();
    UUID secondOutboxId = UUID.randomUUID();

    UserActivityOutbox firstOutbox = mock(UserActivityOutbox.class);
    UserActivityOutbox secondOutbox = mock(UserActivityOutbox.class);

    given(firstOutbox.getId()).willReturn(firstOutboxId);
    given(secondOutbox.getId()).willReturn(secondOutboxId);

    // FAILED 상태이면서 retryCount < 3 조건의 이벤트만 복구 대상으로 조회해야 한다.
    given(userActivityOutboxRepository.findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc(
        UserActivityOutboxStatus.FAILED,
        3,
        PageRequest.of(0, 100)
    )).willReturn(List.of(firstOutbox, secondOutbox));

    // when
    userActivityOutboxProcessor.retryFailedEvents();

    // then
    // 재시도 대상 FAILED 이벤트 조회가 지정된 조건으로 수행되었는지 검증한다.
    verify(userActivityOutboxRepository)
        .findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc(
            UserActivityOutboxStatus.FAILED,
            3,
            PageRequest.of(0, 100)
        );

    // 조회된 각 Outbox 이벤트가 개별 복구 처리되어야 한다.
    verify(userActivityOutboxSingleProcessor).resetSingleFailedEvent(firstOutboxId);
    verify(userActivityOutboxSingleProcessor).resetSingleFailedEvent(secondOutboxId);
  }

  @Test
  @DisplayName("FAILED Outbox 이벤트 중 하나의 복구에 실패해도 다음 이벤트 복구는 계속한다")
  void retryFailedEvents_continuesWhenResetFails() {
    // given
    UUID firstOutboxId = UUID.randomUUID();
    UUID secondOutboxId = UUID.randomUUID();

    UserActivityOutbox firstOutbox = mock(UserActivityOutbox.class);
    UserActivityOutbox secondOutbox = mock(UserActivityOutbox.class);

    given(firstOutbox.getId()).willReturn(firstOutboxId);
    given(secondOutbox.getId()).willReturn(secondOutboxId);

    given(userActivityOutboxRepository.findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc(
        UserActivityOutboxStatus.FAILED,
        3,
        PageRequest.of(0, 100)
    )).willReturn(List.of(firstOutbox, secondOutbox));

    // 첫 번째 이벤트 복구에서 예외가 발생하더라도 다음 이벤트 복구는 계속되어야 한다.
    willThrow(new IllegalStateException("single reset failed"))
        .given(userActivityOutboxSingleProcessor)
        .resetSingleFailedEvent(firstOutboxId);

    // when
    userActivityOutboxProcessor.retryFailedEvents();

    // then
    // 첫 번째 이벤트 복구 실패가 발생했더라도 두 번째 이벤트 복구까지 시도해야 한다.
    verify(userActivityOutboxSingleProcessor).resetSingleFailedEvent(firstOutboxId);
    verify(userActivityOutboxSingleProcessor).resetSingleFailedEvent(secondOutboxId);
  }
}
