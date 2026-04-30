package com.springboot.monew.common.log;

import java.io.File;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
@RequiredArgsConstructor
public class LogUploadBatchService {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager platformTransactionManager;
  private final LogFileItemReader logFileItemReader;
  private final S3UploadItemWriter s3UploadItemWriter;

  @Bean
  public Job logUploadJob(Step logUploadStep){
    return new JobBuilder("logUploadJob", jobRepository)
        .start(logUploadStep)
        .build();
  }

  @Bean
  public Step logUploadStep(){
    return new StepBuilder("logUploadStep", jobRepository)
        .<File, File>chunk(10, platformTransactionManager)
        .reader(logFileItemReader)
        .writer(s3UploadItemWriter)
        .build();
  }


}
