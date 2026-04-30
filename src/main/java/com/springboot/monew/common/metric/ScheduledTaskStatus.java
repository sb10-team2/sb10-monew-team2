package com.springboot.monew.common.metric;

// 정기 작업 실행 결과 상태와 Prometheus 태그 값의 매핑
public enum ScheduledTaskStatus {
  // 정기 작업 정상 완료 상태
  SUCCESS(MonewMetricTags.SUCCESS),

  // 정기 작업 전체 실패 상태
  FAILURE(MonewMetricTags.FAILURE),

  // 정기 작업 부분 실패 상태
  PARTIAL_FAILURE(MonewMetricTags.PARTIAL_FAILURE);

  // status 태그에 기록할 문자열 값
  private final String tagValue;

  ScheduledTaskStatus(String tagValue) {
    this.tagValue = tagValue;
  }

  // Prometheus status 태그 값 반환
  public String tagValue() {
    return tagValue;
  }
}
