package com.springboot.monew.newsarticle.metric;

import com.springboot.monew.common.metric.MetricSupport;
import com.springboot.monew.common.metric.MonewMetricTags;
import com.springboot.monew.newsarticle.metric.result.NewsArticleCollectResult;
import com.springboot.monew.newsarticle.metric.result.NewsArticleSourceCollectResult;
import com.springboot.monew.newsarticle.enums.ArticleSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// 뉴스 수집 스케줄러 도메인 메트릭 기록기
@Component
public class NewsCollectMetrics {

  // Micrometer 메트릭 등록과 기록을 위한 저장소
  private final MeterRegistry meterRegistry;

  // 마지막 뉴스 수집 실행의 관심사 키워드 수 
  private final AtomicLong lastKeywordCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 전체 source 수 
  private final AtomicLong lastSourceCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 성공 source 수 
  private final AtomicLong lastSuccessSourceCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 외부 수집 기사 수 
  private final AtomicLong lastCollectedCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 관심사 매칭 기사 수 
  private final AtomicLong lastMatchedCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 신규 저장 기사 수 
  private final AtomicLong lastSavedCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 중복 제외 기사 수 
  private final AtomicLong lastDuplicateCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 신규 기사-관심사 연결 수 
  private final AtomicLong lastSavedArticleInterestCount = new AtomicLong(0L);

  // 마지막 뉴스 수집 실행의 실패 source 수 
  private final AtomicLong lastFailedSourceCount = new AtomicLong(0L);

  // MeterRegistry 의존성 주입과 Gauge 최초 등록
  public NewsCollectMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    registerLastValueGauges();
  }

  // 뉴스 수집 전체 결과 기준 마지막 실행 Gauge 갱신
  public void recordCollectResult(NewsArticleCollectResult result) {
    if (result == null) {
      recordCollectFailure(0L);
      return;
    }

    updateLastValues(result.keywordCount(), result.sourceCount(), result.successSourceCount(),
        result.totalCollectedArticleCount(),
        result.totalMatchedArticleCount(), result.totalSavedArticleCount(),
        result.totalDuplicateArticleCount(), result.totalSavedArticleInterestCount(),
        result.failedSourceCount());
  }

  // 뉴스 수집 전체 실패 기준 마지막 실행 Gauge 갱신
  public void recordCollectFailure(long failedSourceCount) {
    updateLastValues(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, failedSourceCount);
  }

  // source별 수집 결과 기준 성공 또는 실패 메트릭 기록
  public void recordSource(NewsArticleSourceCollectResult result) {
    if (result == null) {
      recordSourceFailure(null, Duration.ZERO);
      return;
    }

    if (!result.success()) {
      recordSourceFailure(result.source(), result.duration());
      return;
    }

    recordSourceSuccess(result.source(), result.duration(), result.collectedArticleCount(),
        result.matchedArticleCount(), result.savedArticleCount(), result.duplicateArticleCount(),
        result.savedArticleInterestCount());
  }

  // source별 뉴스 수집 성공 처리량과 실행 시간 기록
  public void recordSourceSuccess(ArticleSource source, Duration duration, long collectedCount,
      long matchedCount, long savedCount, long duplicateCount, long savedArticleInterestCount) {
    String sourceName = MetricSupport.enumTag(source);
    recordSourceRun(sourceName, MonewMetricTags.SUCCESS, duration);
    increment(NewsMetricNames.NEWS_COLLECT_COLLECTED_ARTICLES,
        "Collected article count by source", sourceName, collectedCount);
    increment(NewsMetricNames.NEWS_COLLECT_MATCHED_ARTICLES,
        "Interest matched article count by source", sourceName, matchedCount);
    increment(NewsMetricNames.NEWS_COLLECT_SAVED_ARTICLES,
        "Saved article count by source", sourceName, savedCount);
    increment(NewsMetricNames.NEWS_COLLECT_DUPLICATE_ARTICLES,
        "Duplicate article count by source", sourceName, duplicateCount);
    increment(NewsMetricNames.NEWS_COLLECT_SAVED_ARTICLE_INTERESTS,
        "Saved article interest count by source", sourceName, savedArticleInterestCount);
  }

  // source별 뉴스 수집 실패 실행 시간 기록
  public void recordSourceFailure(ArticleSource source, Duration duration) {
    recordSourceRun(MetricSupport.enumTag(source), MonewMetricTags.FAILURE, duration);
  }

  // source별 실행 횟수 Counter와 실행 시간 Timer 기록
  private void recordSourceRun(String sourceName, String status, Duration duration) {
    Counter.builder(NewsMetricNames.NEWS_COLLECT_SOURCE_RUNS)
        .description("News collect source run count")
        .tag(MonewMetricTags.SOURCE, sourceName)
        .tag(MonewMetricTags.STATUS, status)
        .register(meterRegistry)
        .increment();

    Timer.builder(NewsMetricNames.NEWS_COLLECT_SOURCE_DURATION)
        .description("News collect source execution duration")
        .tag(MonewMetricTags.SOURCE, sourceName)
        .tag(MonewMetricTags.STATUS, status)
        .register(meterRegistry)
        .record(MetricSupport.safeDuration(duration));
  }

  // source 태그가 붙은 0보다 큰 Counter 증가량 기록
  private void increment(String metricName, String description, String sourceName, long amount) {
    if (amount <= 0L) {
      return;
    }

    Counter.builder(metricName)
        .description(description)
        .tag(MonewMetricTags.SOURCE, sourceName)
        .register(meterRegistry)
        .increment(amount);
  }

  // 마지막 뉴스 수집 실행 결과 Gauge 값 갱신
  private void updateLastValues(long keywordCount, long sourceCount, long successSourceCount,
      long collectedCount, long matchedCount, long savedCount, long duplicateCount,
      long savedArticleInterestCount, long failedSourceCount) {
    lastKeywordCount.set(MetricSupport.nonNegative(keywordCount));
    lastSourceCount.set(MetricSupport.nonNegative(sourceCount));
    lastSuccessSourceCount.set(MetricSupport.nonNegative(successSourceCount));
    lastCollectedCount.set(MetricSupport.nonNegative(collectedCount));
    lastMatchedCount.set(MetricSupport.nonNegative(matchedCount));
    lastSavedCount.set(MetricSupport.nonNegative(savedCount));
    lastDuplicateCount.set(MetricSupport.nonNegative(duplicateCount));
    lastSavedArticleInterestCount.set(MetricSupport.nonNegative(savedArticleInterestCount));
    lastFailedSourceCount.set(MetricSupport.nonNegative(failedSourceCount));
  }

  // 마지막 뉴스 수집 실행 결과 Gauge 등록
  private void registerLastValueGauges() {
    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_KEYWORD_COUNT, lastKeywordCount,
            AtomicLong::get)
        .description("Keyword count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_SOURCE_COUNT, lastSourceCount,
            AtomicLong::get)
        .description("Source count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_SUCCESS_SOURCE_COUNT, lastSuccessSourceCount,
            AtomicLong::get)
        .description("Successful source count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_COLLECTED_COUNT, lastCollectedCount,
            AtomicLong::get)
        .description("Collected article count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_MATCHED_COUNT, lastMatchedCount,
            AtomicLong::get)
        .description("Interest matched article count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_SAVED_COUNT, lastSavedCount,
            AtomicLong::get)
        .description("Saved article count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_DUPLICATE_COUNT, lastDuplicateCount,
            AtomicLong::get)
        .description("Duplicate article count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_SAVED_ARTICLE_INTEREST_COUNT,
            lastSavedArticleInterestCount, AtomicLong::get)
        .description("Saved article interest count in last news collect run")
        .register(meterRegistry);

    Gauge.builder(NewsMetricNames.NEWS_COLLECT_LAST_FAILED_SOURCE_COUNT, lastFailedSourceCount,
            AtomicLong::get)
        .description("Failed source count in last news collect run")
        .register(meterRegistry);
  }
}
