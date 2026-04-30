package com.springboot.monew.metric;

// Prometheus 메트릭에서 사용하는 공통 태그 이름과 태그 값 모음
public final class MonewMetricTags {

  // 정기 작업 이름을 구분하는 태그 이름
  public static final String TASK = "task";

  // 실행 결과를 구분하는 태그 이름
  public static final String STATUS = "status";

  // 뉴스 수집 source를 구분하는 태그 이름
  public static final String SOURCE = "source";

  // 정상 완료 상태 값
  public static final String SUCCESS = "success";

  // 전체 실패 상태 값
  public static final String FAILURE = "failure";

  // 일부 처리 대상만 실패한 부분 실패 상태 값
  public static final String PARTIAL_FAILURE = "partial_failure";

  // 태그 값을 알 수 없을 때 사용하는 기본 값
  public static final String UNKNOWN = "unknown";

  private MonewMetricTags() {
  }
}
