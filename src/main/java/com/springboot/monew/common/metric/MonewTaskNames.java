package com.springboot.monew.common.metric;

// 대시보드 태그 값으로 사용하는 정기 작업 이름 모음
public final class MonewTaskNames {

  // 뉴스 기사 수집 작업 이름
  public static final String NEWS_COLLECT = "news_collect";

  // 뉴스 기사 S3 백업 작업 이름
  public static final String NEWS_BACKUP = "news_backup";

  // 오래된 알림 삭제 작업 이름
  public static final String NOTIFICATION_CLEANUP = "notification_cleanup";

  // 논리 삭제 후 24시간이 지난 유저 물리 삭제 작업 이름
  public static final String USER_CLEANUP = "user_cleanup";

  private MonewTaskNames() {
  }
}
