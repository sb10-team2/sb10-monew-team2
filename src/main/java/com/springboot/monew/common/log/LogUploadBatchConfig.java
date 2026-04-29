package com.springboot.monew.common.log;

import com.springboot.monew.newsarticles.s3.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
public class LogUploadBatchConfig {

  private final S3Client s3Client;
  private final AwsProperties props;

  @Bean
  @StepScope
  public LogFileItemReader logFileItemReader(
      @Value("#{jobParameters['targetDate']}") String targetDate) {
    return new LogFileItemReader(targetDate);
  }

  @Bean
  @StepScope
  public S3UploadItemWriter s3UploadItemWriter() {
    return new S3UploadItemWriter(s3Client, props);
  }
}
