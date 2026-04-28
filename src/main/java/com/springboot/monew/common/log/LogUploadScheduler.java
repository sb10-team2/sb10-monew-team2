package com.springboot.monew.common.log;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

  private final JobLauncher jobLauncher;
  private final Job logUploadJob;

  @Scheduled(cron = "0 5 0 * * *")  // 매일 00:05
  public void run() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addString("targetDate", LocalDate.now().minusDays(1).toString())
        .addLocalDateTime("runAt", LocalDateTime.now())  // 매번 새 실행 보장
        .toJobParameters();

    jobLauncher.run(logUploadJob, params);
  }
}
