package com.springboot.monew.metric.user;

import com.springboot.monew.metric.MetricSupport;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// 유저 정리 스케줄러 도메인 메트릭 기록기
@Component
public class UserMetrics {

  // Micrometer 메트릭 등록과 기록을 위한 저장소
  private final MeterRegistry meterRegistry;

  // 마지막 유저 정리 작업에서 물리 삭제한 유저 수
  private final AtomicLong lastDeletedCount = new AtomicLong(0L);

  // MeterRegistry 의존성 주입과 Gauge 최초 등록
  public UserMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    registerLastValueGauges();
  }

  // 유저 정리 성공 결과의 삭제 수 기록
  public void recordCleanupSuccess(long deletedCount) {
    increment(UserMetricNames.USER_CLEANUP_DELETED, "Deleted user count by cleanup scheduler",
        deletedCount);
    incrementDeletedRunIfNeeded(deletedCount);
    lastDeletedCount.set(MetricSupport.nonNegative(deletedCount));
  }

  // 0보다 큰 Counter 증가량만 기록
  private void increment(String metricName, String description, long amount) {
    if (amount <= 0L) {
      return;
    }

    Counter.builder(metricName)
        .description(description)
        .register(meterRegistry)
        .increment(amount);
  }

  // 실제 삭제가 발생한 유저 정리 실행 횟수 기록
  private void incrementDeletedRunIfNeeded(long deletedCount) {
    if (deletedCount <= 0L) {
      return;
    }

    Counter.builder(UserMetricNames.USER_CLEANUP_DELETED_RUNS)
        .description("User cleanup run count with deleted users")
        .register(meterRegistry)
        .increment();
  }

  // 마지막 유저 정리 결과 Gauge 등록
  private void registerLastValueGauges() {
    Gauge.builder(UserMetricNames.USER_CLEANUP_LAST_DELETED_COUNT, lastDeletedCount,
            AtomicLong::get)
        .description("Deleted user count in last cleanup run")
        .register(meterRegistry);
  }
}
