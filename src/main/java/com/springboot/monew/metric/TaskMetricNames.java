package com.springboot.monew.metric;

// 정기 작업 공통 메트릭 이름 모음
public final class TaskMetricNames {

  // 정기 작업 실행 횟수
  public static final String TASK_RUNS = "monew.scheduler.task.runs";

  // 정기 작업 실행 시간
  public static final String TASK_DURATION = "monew.scheduler.task.duration";

  // 정기 작업 마지막 실행 시각
  public static final String TASK_LAST_RUN_TIMESTAMP = "monew.scheduler.task.last.run.timestamp";

  // 정기 작업 마지막 성공 시각
  public static final String TASK_LAST_SUCCESS_TIMESTAMP = "monew.scheduler.task.last.success.timestamp";

  // 정기 작업 마지막 실패 시각
  public static final String TASK_LAST_FAILURE_TIMESTAMP = "monew.scheduler.task.last.failure.timestamp";

  private TaskMetricNames() {
  }
}
