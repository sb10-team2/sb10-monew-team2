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

}