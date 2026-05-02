package com.springboot.monew.user.outbox.enums;

// Outbox 이벤트 처리 상태(미처리, 처리 완료, 처리 실패)
public enum UserActivityOutboxStatus {
  PENDING,
  PROCESSED,
  FAILED
}
