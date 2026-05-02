package com.springboot.monew.user.repository;

import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityOutboxRepository extends JpaRepository<UserActivityOutbox, UUID> {
  // 처리 대상 Outbox 이벤트를 지정한 개수만큼만 발생 시각 순으로 조회한다.
  List<UserActivityOutbox> findAllByStatusOrderByOccurredAtAsc(
      UserActivityOutboxStatus status,
      Pageable pageable
  );
  // 재시도 한도를 넘지 않은 FAILED 이벤트만 지정한 개수만큼 조회한다.
  List<UserActivityOutbox> findAllByStatusAndRetryCountLessThanOrderByOccurredAtAsc(
      UserActivityOutboxStatus status,
      int retryCount,
      Pageable pageable
  );
}
