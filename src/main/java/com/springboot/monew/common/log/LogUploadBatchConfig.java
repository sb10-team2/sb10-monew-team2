package com.springboot.monew.common.log;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogUploadBatchConfig {
  @Bean
  @StepScope
  public LogFileItemReader logFileItemReader(
      @Value("#{jobParameters['targetDate']}") String targetDate) {
    return new LogFileItemReader(targetDate);
  }

  @Bean
  @StepScope
  public LogCompressProcessor logCompressProcessor() {
    return new LogCompressProcessor();
  }

  @Bean
  @StepScope
  public S3UploadItemWriter s3UploadItemWriter() {
    return new S3UploadItemWriter();
  }
}
