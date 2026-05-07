package com.springboot.monew.newsarticle.scheduler;

import com.springboot.monew.common.metric.MonewTaskNames;
import com.springboot.monew.common.metric.ScheduledTaskMetrics;
import com.springboot.monew.newsarticle.metric.NewsBackupMetrics;
import com.springboot.monew.newsarticle.metric.result.NewsBackupFileResult;
import com.springboot.monew.newsarticle.metric.result.NewsBackupRunResult;
import com.springboot.monew.newsarticle.s3.NewsArticleBackupService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleBackupScheduler {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final int BACKUP_LOOKBACK_DAYS = 7;

  private final NewsArticleBackupService backupService;
  private final ScheduledTaskMetrics scheduledTaskMetrics;
  private final NewsBackupMetrics newsBackupMetrics;

  //한국시간으로 매일 02:00 실행
  @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
  public void backupYesterday() {
    Instant startedAt = Instant.now();

    //어제 데이터를 백업
    //4/28 02:00 실행 -> 4/27 기사 백업
    LocalDate yesterday = LocalDate.now(KOREA_ZONE).minusDays(1);

    //최근 7일치 백업 누락 여부를 확인한다.
    //백업 파일이 없는 날짜는 다시 백업한다.
    List<NewsBackupFileResult> fileResults = new ArrayList<>();
    long failedCount = 0L;

    for (int i = 0; i < BACKUP_LOOKBACK_DAYS; i++) {
      LocalDate backupDate = yesterday.minusDays(i);

      try {
        fileResults.add(backupService.backupIfMissing(backupDate));
      } catch (Exception e) {
        failedCount++;
        log.error("뉴스 기사 백업 실패. backupDate={}", backupDate, e);
      }
    }

    Duration duration = Duration.between(startedAt, Instant.now());
    NewsBackupRunResult result = new NewsBackupRunResult(BACKUP_LOOKBACK_DAYS, fileResults,
        failedCount);
    newsBackupMetrics.recordRunResult(result);
    scheduledTaskMetrics.record(MonewTaskNames.NEWS_BACKUP, result.scheduledTaskStatus(), duration);
  }
}
