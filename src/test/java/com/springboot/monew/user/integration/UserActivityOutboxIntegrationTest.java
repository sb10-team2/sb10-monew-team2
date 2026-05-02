package com.springboot.monew.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.response.UserDto;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.UserActivityOutboxProcessor;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.outbox.payload.user.UserNicknameUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserRegisteredPayload;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.repository.UserActivityRepository;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserActivityOutboxService;
import com.springboot.monew.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserActivityOutboxIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserActivityRepository userActivityRepository;

  @Autowired
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Autowired
  private UserActivityOutboxService userActivityOutboxService;

  @Autowired
  private UserActivityOutboxProcessor userActivityOutboxProcessor;

  @BeforeEach
  void setUp() {
    // 테스트 간 데이터 격리를 위해 Mongo 사용자 활동 문서와 Outbox, 사용자 데이터를 초기화한다.
    userActivityRepository.deleteAll();
    userActivityOutboxRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("사용자 활동 반영 실패 이후 retry를 실행하면 FAILED에서 PENDING으로 복구되고 재처리 후 PROCESSED로 반영된다")
  void retryFailedOutbox_resetsFailedEventAndProcessesIt() {
    // given
    UserDto user = userService.register(new UserRegisterRequest(
        "retry@test.com",
        "beforeRetry",
        "password123!"
    ));

    // 회원가입 시 생성된 사용자 활동 문서를 제거해 이후 닉네임 갱신 이벤트가 실패하도록 만든다.
    userActivityRepository.deleteById(user.id());
    userActivityOutboxRepository.deleteAll();

    UserActivityOutbox createdOutbox = userActivityOutboxService.save(
        UserActivityEventType.USER_NICKNAME_UPDATED,
        UserActivityAggregateType.USER,
        user.id(),
        new UserNicknameUpdatedPayload(user.id(), "afterRetry")
    );

    // when
    // 실제 Outbox Processor를 실행해 활동 문서가 없는 상태에서 반영 실패를 발생시킨다.
    userActivityOutboxProcessor.processPendingEvents();

    // then
    UserActivityOutbox failedOutbox = userActivityOutboxRepository.findById(createdOutbox.getId()).orElseThrow();
    assertThat(failedOutbox.getStatus()).isEqualTo(UserActivityOutboxStatus.FAILED);
    assertThat(failedOutbox.getRetryCount()).isEqualTo(1);
    assertThat(failedOutbox.getLastError()).isNotBlank();

    // retry 대상 문서를 다시 만들고 복구 스케줄러를 실행하면 FAILED 상태가 PENDING으로 되돌아가야 한다.
    userActivityRepository.save(new UserActivityDocument(
        user.id(),
        user.email(),
        user.nickname(),
        user.createdAt()
    ));

    userActivityOutboxProcessor.retryFailedEvents();

    UserActivityOutbox resetOutbox = userActivityOutboxRepository.findById(createdOutbox.getId()).orElseThrow();
    assertThat(resetOutbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(resetOutbox.getRetryCount()).isEqualTo(1);
    assertThat(resetOutbox.getLastError()).isNull();

    // 복구된 PENDING 이벤트를 다시 처리하면 최종적으로 PROCESSED 상태와 닉네임 반영이 완료되어야 한다.
    userActivityOutboxProcessor.processPendingEvents();

    UserActivityOutbox processedOutbox = userActivityOutboxRepository.findById(createdOutbox.getId()).orElseThrow();
    assertThat(processedOutbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PROCESSED);
    assertThat(processedOutbox.getRetryCount()).isEqualTo(1);
    assertThat(processedOutbox.getProcessedAt()).isNotNull();
    assertThat(processedOutbox.getLastError()).isNull();

    UserActivityDocument activity = userActivityRepository.findById(user.id()).orElseThrow();
    assertThat(activity.getNickname()).isEqualTo("afterRetry");
  }

  @Test
  @DisplayName("이미 활동 문서가 존재하는 상태에서 USER_REGISTERED Outbox 이벤트를 재실행해도 기존 문서를 덮어쓰지 않고 멱등하게 처리한다")
  void processDuplicateUserRegisteredEvent_doesNotOverwriteExistingUserActivity() {
    // given
    UserDto user = userService.register(new UserRegisterRequest(
        "idempotent@test.com",
        "originalNickname",
        "password123!"
    ));

    UserActivityDocument beforeReplay = userActivityRepository.findById(user.id()).orElseThrow();
    Instant originalCreatedAt = beforeReplay.getCreatedAt();

    // 회원가입 시 생성된 원래 Outbox 이벤트는 검증 대상에서 제외하고 재실행용 이벤트만 남긴다.
    userActivityOutboxRepository.deleteAll();

    UserActivityOutbox replayOutbox = userActivityOutboxService.save(
        UserActivityEventType.USER_REGISTERED,
        UserActivityAggregateType.USER,
        user.id(),
        new UserRegisteredPayload(
            user.id(),
            "replayed@test.com",
            "replayedNickname",
            originalCreatedAt.plusSeconds(60)
        )
    );

    // when
    // 이미 활동 문서가 존재하는 상태에서 USER_REGISTERED 이벤트를 다시 처리해 멱등 동작을 검증한다.
    userActivityOutboxProcessor.processPendingEvents();

    // then
    UserActivityOutbox processedOutbox = userActivityOutboxRepository.findById(replayOutbox.getId()).orElseThrow();
    assertThat(processedOutbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PROCESSED);
    assertThat(processedOutbox.getRetryCount()).isZero();
    assertThat(processedOutbox.getLastError()).isNull();

    // 기존 사용자 활동 문서는 재실행 payload 값으로 덮어써지지 않고 최초 생성 값이 유지되어야 한다.
    UserActivityDocument activity = userActivityRepository.findById(user.id()).orElseThrow();
    assertThat(activity.getEmail()).isEqualTo(user.email());
    assertThat(activity.getNickname()).isEqualTo(user.nickname());
    assertThat(activity.getCreatedAt()).isEqualTo(originalCreatedAt);
    assertThat(activity.getSubscriptions()).isEmpty();
    assertThat(activity.getComments()).isEmpty();
    assertThat(activity.getCommentLikes()).isEmpty();
    assertThat(activity.getArticleViews()).isEmpty();

    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);
  }
}
