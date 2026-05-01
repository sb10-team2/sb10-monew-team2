package com.springboot.monew.user.outbox;

import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserActivityOutboxProcessor {
  // 동일 이벤트를 무한 재시도하지 않도록 최대 재시도 횟수를 제한한다.
  private static final int MAX_RETRY_COUNT = 3;

  private final UserActivityOutboxRepository userActivityOutboxRepository;
  private final UserActivityOutboxSingleProcessor userActivityOutboxSingleProcessor;

  // 미처리 Outbox 이벤트를 발생 순서대로 처리한다.
  public void processPendingEvents() {
    List<UserActivityOutbox> outboxes =
        userActivityOutboxRepository.findAllByStatusOrderByOccurredAtAsc(
            UserActivityOutboxStatus.PENDING
        );

    for (UserActivityOutbox outbox : outboxes) {
      userActivityOutboxSingleProcessor.processSingleEvent(outbox.getId());
    }
  }

  @Transactional
  public void retryFailedEvents() {
    List<UserActivityOutbox> failedOutboxes =
        userActivityOutboxRepository
            .findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc(
                UserActivityOutboxStatus.FAILED,
                MAX_RETRY_COUNT
            );

    for (UserActivityOutbox outbox : failedOutboxes) {
      // FAILED 이벤트를 다시 처리할 수 있도록 대기 상태로 복원한다.
      // 실제 처리는 다음 processPendingEvents() 주기에서 수행한다.
      outbox.resetToPending();
    }
  }

}