package com.springboot.monew.metric.newsarticle;

import com.springboot.monew.metric.MetricSupport;
import com.springboot.monew.metric.newsarticle.result.NewsBackupRunResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// 뉴스 백업 스케줄러 도메인 메트릭 기록기
@Component
public class NewsBackupMetrics {

  // Micrometer 메트릭 등록과 기록을 위한 저장소
  private final MeterRegistry meterRegistry;

  // 마지막 뉴스 백업 실행의 업로드 파일 수
  private final AtomicLong lastUploadedCount = new AtomicLong(0L);

  // 마지막 뉴스 백업 실행의 확인 날짜 수
  private final AtomicLong lastCheckedDateCount = new AtomicLong(0L);

  // 마지막 뉴스 백업 실행의 스킵 파일 수
  private final AtomicLong lastSkippedCount = new AtomicLong(0L);

  // 마지막 뉴스 백업 실행의 실패 날짜 수
  private final AtomicLong lastFailedCount = new AtomicLong(0L);

  // 마지막 뉴스 백업 실행의 백업 기사 수
  private final AtomicLong lastArticleCount = new AtomicLong(0L);

  // 마지막 뉴스 백업 실행의 업로드 payload byte 크기 저장소
  private final AtomicLong lastPayloadBytes = new AtomicLong(0L);

  // MeterRegistry 의존성 주입과 Gauge 최초 등록
  public NewsBackupMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    registerLastValueGauges();
  }

  // 뉴스 백업 전체 실행 결과 기준 Counter와 Gauge 기록
  public void recordRunResult(NewsBackupRunResult result) {
    if (result == null) {
      recordBackupResult(0L, 0L, 0L, 0L, 0L, 0L);
      return;
    }

    recordBackupResult(result.checkedDateCount(), result.uploadedFileCount(),
        result.skippedFileCount(), result.failedCount(), result.totalArticleCount(),
        result.totalPayloadBytes());
  }

  // 뉴스 백업 집계 값 기반 도메인 메트릭 기록
  private void recordBackupResult(long checkedDateCount, long uploadedCount, long skippedCount,
      long failedCount, long articleCount, long payloadBytes) {
    increment(NewsMetricNames.NEWS_BACKUP_CHECKED_DATES, "Backup checked date count",
        checkedDateCount);
    increment(NewsMetricNames.NEWS_BACKUP_UPLOADED_FILES, "Uploaded backup file count",
        uploadedCount);
    increment(NewsMetricNames.NEWS_BACKUP_SKIPPED_FILES, "Skipped existing backup file count",
        skippedCount);
    increment(NewsMetricNames.NEWS_BACKUP_FAILED_FILES, "Failed backup date count", failedCount);
    increment(NewsMetricNames.NEWS_BACKUP_ARTICLES, "Articles in uploaded backup files",
        articleCount);
    incrementBytes(NewsMetricNames.NEWS_BACKUP_PAYLOAD_BYTES, "Uploaded backup payload size",
        payloadBytes);
    updateLastValues(checkedDateCount, uploadedCount, skippedCount, failedCount, articleCount,
        payloadBytes);
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

  // 0보다 큰 byte Counter 증가량만 기록
  private void incrementBytes(String metricName, String description, long amount) {
    if (amount <= 0L) {
      return;
    }

    Counter.builder(metricName)
        .description(description)
        .baseUnit("bytes")
        .register(meterRegistry)
        .increment(amount);
  }

  // 마지막 뉴스 백업 실행 결과 Gauge 값 갱신
  private void updateLastValues(long checkedDateCount, long uploadedCount, long skippedCount,
      long failedCount, long articleCount, long payloadBytes) {
    lastCheckedDateCount.set(MetricSupport.nonNegative(checkedDateCount));
    lastUploadedCount.set(MetricSupport.nonNegative(uploadedCount));
    lastSkippedCount.set(MetricSupport.nonNegative(skippedCount));
    lastFailedCount.set(MetricSupport.nonNegative(failedCount));
    lastArticleCount.set(MetricSupport.nonNegative(articleCount));
    lastPayloadBytes.set(MetricSupport.nonNegative(payloadBytes));
  }

  // 마지막 뉴스 백업 실행 결과 Gauge 등록
  private void registerLastValueGauges() {
    Gauge.builder(NewsMetricNames.NEWS_BACKUP_LAST_CHECKED_DATE_COUNT, lastCheckedDateCount,
            AtomicLong::get)
        .description("Last backup checked date count")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_BACKUP_LAST_UPLOADED_COUNT, lastUploadedCount,
            AtomicLong::get)
        .description("Last backup uploaded file count")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_BACKUP_LAST_SKIPPED_COUNT, lastSkippedCount,
            AtomicLong::get)
        .description("Last backup skipped existing file count")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_BACKUP_LAST_FAILED_COUNT, lastFailedCount,
            AtomicLong::get)
        .description("Last backup failed date count")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_BACKUP_LAST_ARTICLE_COUNT, lastArticleCount,
            AtomicLong::get)
        .description("Last backup article count")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_BACKUP_LAST_PAYLOAD_BYTES, lastPayloadBytes,
            AtomicLong::get)
        .description("Last backup payload size")
        .baseUnit("bytes")
        .register(meterRegistry);
  }
}
