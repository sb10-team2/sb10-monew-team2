package com.springboot.monew.user.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.outbox.payload.comment.CommentDeletedPayload;
import com.springboot.monew.user.outbox.payload.user.UserRegisteredPayload;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.service.UserActivityUpdateService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityOutboxSingleProcessorTest {

  @Mock
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Mock
  private UserActivityOutboxPayloadSerializer payloadSerializer;

  @Mock
  private UserActivityUpdateService userActivityUpdateService;

  @InjectMocks
  private UserActivityOutboxSingleProcessor userActivityOutboxSingleProcessor;

  @Test
  @DisplayName("USER_REGISTERED Outbox 이벤트 처리 성공 시 사용자 활동 문서를 생성하고 상태를 PROCESSED로 변경한다")
  void processSingleEvent_userRegistered_success() {
    // given
    UUID outboxId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-05-01T00:00:00Z");
    Instant createdAt = Instant.parse("2026-05-01T00:00:00Z");

    UserActivityOutbox outbox = new UserActivityOutbox(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        userId,
        "{\"userId\":\"" + userId + "\"}",
        occurredAt
    );

    UserRegisteredPayload payload = new UserRegisteredPayload(
        userId,
        "test@example.com",
        "tester",
        createdAt
    );

    given(userActivityOutboxRepository.findById(outboxId))
        .willReturn(Optional.of(outbox));

    // USER_REGISTERED payload JSON을 역직렬화했을 때 사용할 객체를 미리 준비한다.
    given(payloadSerializer.fromJson(outbox.getPayload(), UserRegisteredPayload.class))
        .willReturn(payload);

    // when
    userActivityOutboxSingleProcessor.processSingleEvent(outboxId);

    // then
    // USER_REGISTERED 이벤트이므로 사용자 활동 문서 생성 메서드가 호출되어야 한다.
    verify(userActivityUpdateService).createUserActivity(payload);

    // 처리 성공 후 Outbox 상태가 PROCESSED로 변경되어야 한다.
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PROCESSED);

    // 처리 완료 시각이 기록되어야 한다.
    assertThat(outbox.getProcessedAt()).isNotNull();

    // 성공 처리 후 마지막 에러 메시지는 비워져야 한다.
    assertThat(outbox.getLastError()).isNull();
  }

  @Test
  @DisplayName("Outbox 이벤트 처리 중 사용자 활동 반영에 실패하면 상태를 FAILED로 변경하고 재시도 횟수를 증가시킨다")
  void processSingleEvent_whenUpdateServiceFails_marksFailed() {
    // given
    UUID outboxId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    UserActivityOutbox outbox = new UserActivityOutbox(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        userId,
        "{\"userId\":\"" + userId + "\"}",
        Instant.parse("2026-05-01T00:00:00Z")
    );

    UserRegisteredPayload payload = new UserRegisteredPayload(
        userId,
        "test@example.com",
        "tester",
        Instant.parse("2026-05-01T00:00:00Z")
    );

    given(userActivityOutboxRepository.findById(outboxId))
        .willReturn(Optional.of(outbox));

    given(payloadSerializer.fromJson(outbox.getPayload(), UserRegisteredPayload.class))
        .willReturn(payload);

    // Mongo 반영 단계에서 예외가 발생한 상황을 가정한다.
    willThrow(new IllegalStateException("mongo failed"))
        .given(userActivityUpdateService)
        .createUserActivity(payload);

    // when
    userActivityOutboxSingleProcessor.processSingleEvent(outboxId);

    // then
    // 처리 실패 시 상태는 FAILED가 되어야 한다.
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.FAILED);

    // 실패 횟수는 1 증가해야 한다.
    assertThat(outbox.getRetryCount()).isEqualTo(1);

    // 마지막 에러 메시지가 저장되어야 한다.
    assertThat(outbox.getLastError()).contains("mongo failed");
  }

  @Test
  @DisplayName("Outbox payload 역직렬화에 실패하면 상태를 FAILED로 변경하고 사용자 활동 반영은 수행하지 않는다")
  void processSingleEvent_whenPayloadDeserializationFails_marksFailed() {
    // given
    UUID outboxId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    UserActivityOutbox outbox = new UserActivityOutbox(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        userId,
        "invalid-json",
        Instant.parse("2026-05-01T00:00:00Z")
    );

    given(userActivityOutboxRepository.findById(outboxId))
        .willReturn(Optional.of(outbox));

    // payload 복원 자체가 실패하는 경우를 가정한다.
    given(payloadSerializer.fromJson(outbox.getPayload(), UserRegisteredPayload.class))
        .willThrow(new IllegalStateException("deserialize failed"));

    // when
    userActivityOutboxSingleProcessor.processSingleEvent(outboxId);

    // then
    // 역직렬화 실패도 Outbox 처리 실패로 기록되어야 한다.
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.FAILED);
    assertThat(outbox.getRetryCount()).isEqualTo(1);
    assertThat(outbox.getLastError()).contains("deserialize failed");

    // payload 복원에 실패했으므로 사용자 활동 반영 로직은 호출되면 안 된다.
    verifyNoInteractions(userActivityUpdateService);
  }

  @Test
  @DisplayName("FAILED 상태 Outbox 이벤트를 재시도할 수 있도록 PENDING 상태로 복구한다")
  void resetSingleFailedEvent_resetsFailedOutboxToPending() {
    // given
    UUID outboxId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    UserActivityOutbox outbox = new UserActivityOutbox(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        userId,
        "{\"userId\":\"" + userId + "\"}",
        Instant.parse("2026-05-01T00:00:00Z")
    );
    outbox.markFailed("temporary failure");

    given(userActivityOutboxRepository.findById(outboxId))
        .willReturn(Optional.of(outbox));

    // when
    userActivityOutboxSingleProcessor.resetSingleFailedEvent(outboxId);

    // then
    // 재시도를 위해 상태가 다시 PENDING으로 돌아가야 한다.
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);

    // 복구 후 마지막 에러 메시지는 제거되어야 한다.
    assertThat(outbox.getLastError()).isNull();

    // 실패 이력은 유지한 채 상태만 재시도 가능하게 되돌린다.
    assertThat(outbox.getRetryCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("처리할 Outbox 이벤트가 존재하지 않으면 예외가 발생한다")
  void processSingleEvent_throwsException_whenOutboxNotFound() {
    // given
    UUID outboxId = UUID.randomUUID();

    // 처리 대상 Outbox 이벤트가 존재하지 않는 상황을 만들기 위해 빈 Optional을 반환하도록 설정한다.
    given(userActivityOutboxRepository.findById(outboxId))
        .willReturn(Optional.empty());

    // when & then
    // 처리할 Outbox 이벤트를 찾지 못하면 IllegalStateException이 발생해야 한다.
    assertThatThrownBy(() -> userActivityOutboxSingleProcessor.processSingleEvent(outboxId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Outbox 이벤트를 찾을 수 없다.");


    verify(userActivityOutboxRepository).findById(outboxId);
    // Outbox를 찾지 못한 시점에서 바로 예외가 발생하므로 payload 복원과 사용자 활동 반영은 수행되면 안 된다.
    verifyNoInteractions(payloadSerializer, userActivityUpdateService);
  }

  @Test
  @DisplayName("복구할 FAILED Outbox 이벤트가 존재하지 않으면 예외가 발생한다")
  void resetSingleFailedEvent_throwsException_whenOutboxNotFound() {
    // given
    UUID outboxId = UUID.randomUUID();

    // 복구 대상 Outbox 이벤트가 존재하지 않는 상황을 만들기 위해 빈 Optional을 반환하도록 설정한다.
    given(userActivityOutboxRepository.findById(outboxId))
        .willReturn(Optional.empty());

    // when & then
    // 복구할 Outbox 이벤트를 찾지 못하면 IllegalStateException이 발생해야 한다.
    assertThatThrownBy(() -> userActivityOutboxSingleProcessor.resetSingleFailedEvent(outboxId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Outbox 이벤트를 찾을 수 없다.");

    // Outbox 조회가 실제로 수행되었는지 검증한다.
    verify(userActivityOutboxRepository).findById(outboxId);

    // Outbox를 찾지 못한 시점에서 바로 예외가 발생하므로 payload 복원과 사용자 활동 반영은 수행되면 안 된다.
    verifyNoInteractions(payloadSerializer, userActivityUpdateService);
  }

  @Test
  @DisplayName("COMMENT_DELETED Outbox 이벤트 처리 성공 시 사용자 활동 문서에서 댓글 내역을 제거하고 상태를 PROCESSED로 변경한다")
  void processSingleEvent_commentDeleted_success() {
    // given
    UUID outboxId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    UserActivityOutbox outbox = new UserActivityOutbox(
        UserActivityEventType.COMMENT_DELETED,
        UserActivityAggregateType.COMMENT,
        commentId,
        "comment-deleted-payload",
        Instant.parse("2026-05-01T00:00:00Z")
    );

    CommentDeletedPayload payload = CommentDeletedPayload.of(userId, commentId);

    // 처리 대상 Outbox 이벤트를 정상 조회하도록 설정한다.
    given(userActivityOutboxRepository.findById(outboxId))
        .willReturn(Optional.of(outbox));

    // COMMENT_DELETED payload JSON을 역직렬화했을 때 사용할 payload 객체를 미리 준비한다.
    given(payloadSerializer.fromJson(outbox.getPayload(), CommentDeletedPayload.class))
        .willReturn(payload);

    // when
    userActivityOutboxSingleProcessor.processSingleEvent(outboxId);

    // then
    // COMMENT_DELETED 이벤트이므로 사용자 활동 문서에서 댓글 내역을 제거하는 메서드가 호출되어야 한다.
    verify(userActivityUpdateService).removeComment(payload);

    // Outbox 조회가 실제로 수행되었는지 검증한다.
    verify(userActivityOutboxRepository).findById(outboxId);

    // payload 복원이 COMMENT_DELETED 타입으로 수행되었는지 검증한다.
    verify(payloadSerializer).fromJson(outbox.getPayload(), CommentDeletedPayload.class);

    // 처리 성공 후 Outbox 상태가 PROCESSED로 변경되어야 한다.
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PROCESSED);

    // 처리 완료 시각이 기록되어야 한다.
    assertThat(outbox.getProcessedAt()).isNotNull();

    // 성공 처리 후 마지막 에러 메시지는 비워져야 한다.
    assertThat(outbox.getLastError()).isNull();
  }
}
