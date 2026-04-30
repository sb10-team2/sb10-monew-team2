package com.springboot.monew.metric.notification;

// 알림 정리 스케줄러 도메인 메트릭 이름 모음
public final class NotificationMetricNames {

  // 알림 정리 작업으로 삭제된 알림 수
  public static final String NOTIFICATION_CLEANUP_DELETED = "monew.notification.cleanup.deleted";

  // 알림 정리 작업에서 삭제가 발생한 chunk 수
  public static final String NOTIFICATION_CLEANUP_CHUNKS = "monew.notification.cleanup.chunks";

  // 마지막 알림 정리 작업의 삭제 알림 수
  public static final String NOTIFICATION_CLEANUP_LAST_DELETED_COUNT = "monew.notification.cleanup.last.deleted.count";

  // 마지막 알림 정리 작업의 삭제 발생 chunk 수
  public static final String NOTIFICATION_CLEANUP_LAST_CHUNK_COUNT = "monew.notification.cleanup.last.chunk.count";

  private NotificationMetricNames() {
  }
}
