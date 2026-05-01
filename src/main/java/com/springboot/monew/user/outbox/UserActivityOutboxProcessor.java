package com.springboot.monew.user.outbox;

import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
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
      userActivityOutboxSingleProcessor.processSingleEvent(outbox.getId());
    }

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
                MAX_RETRY_COUNT,
                PageRequest.of(0, BATCH_SIZE)
            );

    for (UserActivityOutbox outbox : failedOutboxes) {
      outbox.resetToPending();
    }
  }

}