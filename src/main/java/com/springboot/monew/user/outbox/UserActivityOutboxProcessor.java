package com.springboot.monew.user.outbox;

import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserActivityOutboxProcessor {

  // 동일 이벤트를 무한 재시도하지 않도록 최대 재시도 횟수를 제한한다.
  private static final int MAX_RETRY_COUNT = 3;
  private static final int BATCH_SIZE = 100;

  private final UserActivityOutboxRepository userActivityOutboxRepository;
  private final UserActivityOutboxSingleProcessor userActivityOutboxSingleProcessor;

  public void processPendingEvents() {
    List<UserActivityOutbox> outboxes =
        userActivityOutboxRepository.findAllByStatusOrderByOccurredAtAsc(
            UserActivityOutboxStatus.PENDING,
            PageRequest.of(0, BATCH_SIZE)
        );

    for (UserActivityOutbox outbox : outboxes) {
      try {
        userActivityOutboxSingleProcessor.processSingleEvent(outbox.getId());
      } catch (Exception e) {
        log.error("사용자 활동 Outbox 개별 처리 호출 실패 - outboxId={}", outbox.getId(), e);
      }
    }

  }

  public void retryFailedEvents() {
    List<UserActivityOutbox> failedOutboxes =
        userActivityOutboxRepository
            .findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc(
                UserActivityOutboxStatus.FAILED,
                MAX_RETRY_COUNT,
                PageRequest.of(0, BATCH_SIZE)
            );

    for (UserActivityOutbox outbox : failedOutboxes) {
      try {
        userActivityOutboxSingleProcessor.resetSingleFailedEvent(outbox.getId());
      } catch (Exception e) {
        log.error("사용자 활동 Outbox 재시도 복원 실패 - outboxId={}", outbox.getId(), e);
      }
    }
  }

}