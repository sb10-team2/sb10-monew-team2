package com.springboot.monew.user.metric;

// 유저 정리 스케줄러 도메인 메트릭 이름 모음
public final class UserMetricNames {

  // 유저 정리 작업으로 물리 삭제된 유저 수
  public static final String USER_CLEANUP_DELETED = "monew.user.cleanup.deleted";

  // 유저 정리 작업에서 실제 삭제가 발생한 실행 횟수
  public static final String USER_CLEANUP_DELETED_RUNS = "monew.user.cleanup.deleted.runs";

  // 마지막 유저 정리 작업의 물리 삭제 유저 수
  public static final String USER_CLEANUP_LAST_DELETED_COUNT = "monew.user.cleanup.last.deleted.count";

  private UserMetricNames() {
  }
}
