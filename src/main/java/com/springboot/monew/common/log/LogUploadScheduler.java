package com.springboot.monew.common.log;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogUploadScheduler {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private final JobLauncher jobLauncher;
  private final Job logUploadJob;

  @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")  // 매일 00:05 KST
  public void run() throws Exception {
    String targetDate = LocalDate.now(ZONE).minusDays(1).toString();
    JobParameters params = new JobParametersBuilder()
        .addString("targetDate", targetDate)
        .toJobParameters();

    jobLauncher.run(logUploadJob, params);
  }
}
