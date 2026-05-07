package com.springboot.monew.newsarticle.scheduler;

import com.springboot.monew.common.metric.MonewTaskNames;
import com.springboot.monew.common.metric.ScheduledTaskMetrics;
import com.springboot.monew.newsarticle.metric.NewsCollectMetrics;
import com.springboot.monew.newsarticle.metric.result.NewsArticleCollectResult;
import com.springboot.monew.newsarticle.service.NewsArticleCollectService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleScheduler {

  private final NewsArticleCollectService newsArticleCollectService;
  private final ScheduledTaskMetrics scheduledTaskMetrics;
  private final NewsCollectMetrics newsCollectMetrics;

  //매 시간 0분 0초 newsArticleCollectService.collectAll()을 실행한다.
  @Scheduled(cron = "0 0 * * * *")
  public void collectArticlesEveryHour() {
    Instant startedAt = Instant.now();

    log.info("뉴스 기사 수집 배치 시작");

    try {
      NewsArticleCollectResult result = newsArticleCollectService.collectAll();
      result.sourceResults().forEach(newsCollectMetrics::recordSource);
      newsCollectMetrics.recordCollectResult(result);

      Duration duration = Duration.between(startedAt, Instant.now());
      scheduledTaskMetrics.record(MonewTaskNames.NEWS_COLLECT, result.scheduledTaskStatus(),
          duration);
    } catch (Exception e) {
      log.error("뉴스 기사 수집 배치 실패", e);
      newsCollectMetrics.recordCollectFailure(0L);
      scheduledTaskMetrics.recordFailure(MonewTaskNames.NEWS_COLLECT,
          Duration.between(startedAt, Instant.now()));
    }

    log.info("뉴스 기사 수집 배치 종료");
  }
}
