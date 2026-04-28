package com.springboot.monew.newsarticles.scheduler;


import com.springboot.monew.newsarticles.s3.NewsArticleBackupService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleBackupScheduler {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private final NewsArticleBackupService backupService;

  //한국시간으로 매일 02:00 실행
  @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
  public void backupYesterday() {

    //어제 데이터를 백업
    //4/28 02:00 실행 -> 4/27 기사 백업
    LocalDate yesterday = LocalDate.now(KOREA_ZONE).minusDays(1);

    //최근 7일치 백업 누락 여부를 확인한다.
    //백업 파일이 없는 날짜는 다시 백업한다.
    for(int i = 0; i < 7; i++){
      LocalDate backupDate = yesterday.minusDays(i);

      try{
        backupService.backupIfMissing(backupDate);
      }catch (Exception e){
        log.error("뉴스 기사 백업 실패. backupDate={}", backupDate, e);
      }
    }
  }
}
