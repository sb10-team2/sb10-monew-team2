package com.springboot.monew.user.repository;

import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityOutboxRepository extends JpaRepository<UserActivityOutbox, UUID> {
  // 미처리 Outbox 이벤트를 발생 시각 순으로 조회한다.
  List<UserActivityOutbox> findAllByStatusOrderByOccurredAtAsc(UserActivityOutboxStatus status);
}
