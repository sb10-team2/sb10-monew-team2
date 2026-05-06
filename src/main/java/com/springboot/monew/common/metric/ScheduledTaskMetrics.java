package com.springboot.monew.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// 모든 정기 작업에 공통으로 적용되는 실행 메트릭 기록기
@Component
public class ScheduledTaskMetrics {

  // Micrometer 메트릭 등록과 기록을 위한 저장소
  private final MeterRegistry meterRegistry;

  // 작업별 마지막 실행 시각 Gauge 값 저장소
  private final ConcurrentMap<String, AtomicLong> lastRunTimestamps = new ConcurrentHashMap<>();

  // 작업별 마지막 성공 시각 Gauge 값 저장소
  private final ConcurrentMap<String, AtomicLong> lastSuccessTimestamps = new ConcurrentHashMap<>();

  // 작업별 마지막 실패 시각 Gauge 값 저장소
  private final ConcurrentMap<String, AtomicLong> lastFailureTimestamps = new ConcurrentHashMap<>();

  // MeterRegistry 의존성 주입
  public ScheduledTaskMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  // 성공 상태 정기 작업 실행 메트릭 기록
  public void recordSuccess(String taskName, Duration duration) {
    record(taskName, ScheduledTaskStatus.SUCCESS, duration);
  }

  // 실패 상태 정기 작업 실행 메트릭 기록
  public void recordFailure(String taskName, Duration duration) {
    record(taskName, ScheduledTaskStatus.FAILURE, duration);
  }

  // 부분 실패 상태 정기 작업 실행 메트릭 기록
  public void recordPartialFailure(String taskName, Duration duration) {
    record(taskName, ScheduledTaskStatus.PARTIAL_FAILURE, duration);
  }

  // 정기 작업 실행 횟수와 실행 시간 및 마지막 상태 시각 기록
  public void record(String taskName, ScheduledTaskStatus status, Duration duration) {
    String safeTaskName = MetricSupport.tagValue(taskName);
    ScheduledTaskStatus safeStatus = status == null ? ScheduledTaskStatus.FAILURE : status;
    Instant completedAt = Instant.now();

    recordRun(safeTaskName, safeStatus.tagValue(), duration, completedAt);
    if (safeStatus == ScheduledTaskStatus.SUCCESS) {
      updateTimestamp(lastSuccessTimestamps, TaskMetricNames.TASK_LAST_SUCCESS_TIMESTAMP,
          "Last successful scheduled task completion time", safeTaskName, completedAt);
      return;
    }

    updateTimestamp(lastFailureTimestamps, TaskMetricNames.TASK_LAST_FAILURE_TIMESTAMP,
        "Last failed scheduled task completion time", safeTaskName, completedAt);
  }

  // 정기 작업 실행 Counter와 Duration Timer 기록
  private void recordRun(String taskName, String status, Duration duration, Instant completedAt) {
    Counter.builder(TaskMetricNames.TASK_RUNS)
        .description("Scheduled task run count")
        .tag(MonewMetricTags.TASK, taskName)
        .tag(MonewMetricTags.STATUS, status)
        .register(meterRegistry)
        .increment();

    Timer.builder(TaskMetricNames.TASK_DURATION)
        .description("Scheduled task execution duration")
        .tag(MonewMetricTags.TASK, taskName)
        .tag(MonewMetricTags.STATUS, status)
        .register(meterRegistry)
        .record(MetricSupport.safeDuration(duration));

    updateTimestamp(lastRunTimestamps, TaskMetricNames.TASK_LAST_RUN_TIMESTAMP,
        "Last scheduled task completion time", taskName, completedAt);
  }

  // 작업별 timestamp Gauge 값 갱신
  private void updateTimestamp(ConcurrentMap<String, AtomicLong> timestampMap, String metricName,
      String description, String taskName, Instant timestamp) {
    AtomicLong gaugeValue = timestampMap.computeIfAbsent(taskName,
        key -> registerTimestampGauge(metricName, description, key));
    gaugeValue.set(timestamp.getEpochSecond());
  }

  // 작업별 timestamp Gauge 최초 등록
  private AtomicLong registerTimestampGauge(String metricName, String description,
      String taskName) {
    AtomicLong gaugeValue = new AtomicLong(0L);
    Gauge.builder(metricName, gaugeValue, AtomicLong::get)
        .description(description)
        .baseUnit("seconds")
        .tag(MonewMetricTags.TASK, taskName)
        .register(meterRegistry);
    return gaugeValue;
  }
}
