package com.springboot.monew.newsarticles.scheduler;

import com.springboot.monew.newsarticles.s3.NewsArticleBackupService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsArticleBackupScheduler {

  private final NewsArticleBackupService backupService;

  //한국시간으로 매일 02:00 실행
  @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
  public void backupYesterday() {

    //어제 데이터를 백업
    //4/28 02:00 실행 -> 4/27 기사 백업
    LocalDate backupDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    backupService.backupByPublishedAtDate(backupDate);
  }

}
