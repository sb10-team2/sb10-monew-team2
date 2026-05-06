package com.springboot.monew.notification.metric;

import com.springboot.monew.common.metric.MetricSupport;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// 알림 정리 스케줄러 도메인 메트릭 기록기
@Component
public class NotificationMetrics {

  // Micrometer 메트릭 등록과 기록을 위한 저장소
  private final MeterRegistry meterRegistry;

  // 마지막 알림 정리 작업에서 삭제한 알림 수
  private final AtomicLong lastDeletedCount = new AtomicLong(0L);

  // 마지막 알림 정리 작업에서 삭제가 발생한 chunk 수
  private final AtomicLong lastChunkCount = new AtomicLong(0L);

  // MeterRegistry 의존성 주입과 Gauge 최초 등록
  public NotificationMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    registerLastValueGauges();
  }

  // 알림 정리 결과의 삭제 수와 chunk 수 기록
  public void recordCleanupResult(long deletedCount, long chunkCount) {
    increment(NotificationMetricNames.NOTIFICATION_CLEANUP_DELETED,
        "Deleted notification count by cleanup scheduler", deletedCount);
    increment(NotificationMetricNames.NOTIFICATION_CLEANUP_CHUNKS,
        "Notification cleanup chunk count with deleted rows", chunkCount);
    lastDeletedCount.set(MetricSupport.nonNegative(deletedCount));
    lastChunkCount.set(MetricSupport.nonNegative(chunkCount));
  }

  // 알림 정리 성공 결과의 삭제 수와 chunk 수 기록
  public void recordCleanupSuccess(long deletedCount, long chunkCount) {
    recordCleanupResult(deletedCount, chunkCount);
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

  // 마지막 알림 정리 결과 Gauge 등록
  private void registerLastValueGauges() {
    Gauge.builder(NotificationMetricNames.NOTIFICATION_CLEANUP_LAST_DELETED_COUNT,
            lastDeletedCount, AtomicLong::get)
        .description("Deleted notification count in last cleanup run")
        .register(meterRegistry);

    Gauge.builder(NotificationMetricNames.NOTIFICATION_CLEANUP_LAST_CHUNK_COUNT, lastChunkCount,
            AtomicLong::get)
        .description("Cleanup chunk count with deleted rows in last run")
        .register(meterRegistry);
  }
}
