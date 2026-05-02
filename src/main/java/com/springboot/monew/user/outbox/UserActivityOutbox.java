package com.springboot.monew.user.outbox;

import com.springboot.monew.common.entity.BaseUpdatableEntity;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Entity
@Table(name = "user_activity_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivityOutbox extends BaseUpdatableEntity {

  // 사용자 활동 이벤트 타입
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 50)
  private UserActivityEventType eventType;

  // 집계 대상 타입
  @Enumerated(EnumType.STRING)
  @Column(name = "aggregate_type", nullable = false, length = 30)
  private UserActivityAggregateType aggregateType;

  // 집계 대상 식별자
  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  // Mongo 반영에 필요한 JSON payload
  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  // Outbox 처리 상태
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserActivityOutboxStatus status;

  // 재시도 횟수
  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  // 이벤트 발생 시각
  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  // 처리 완료 시각
  @Column(name = "processed_at")
  private Instant processedAt;

  // 마지막 실패 메시지
  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  public UserActivityOutbox(
      UserActivityEventType eventType,
      UserActivityAggregateType aggregateType,
      UUID aggregateId,
      String payload,
      Instant occurredAt
  ) {
    this.eventType = eventType;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.payload = payload;
    this.status = UserActivityOutboxStatus.PENDING;
    this.retryCount = 0;
    this.occurredAt = occurredAt;
  }

  // 처리 성공 상태로 변경
  public void markProcessed(Instant processedAt) {
    this.status = UserActivityOutboxStatus.PROCESSED;
    this.processedAt = processedAt;
    this.lastError = null;
  }

  // 처리 실패 상태로 변경
  public void markFailed(String lastError) {
    this.status = UserActivityOutboxStatus.FAILED;
    this.retryCount++;
    this.lastError = lastError;
  }

  // 재처리를 위해 대기 상태로 복원
  public void resetToPending() {
    this.status = UserActivityOutboxStatus.PENDING;
    this.lastError = null;
  }
}
