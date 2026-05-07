package com.springboot.monew.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

import com.springboot.monew.newsarticle.s3.AwsProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SpringBatchTest
@SpringBootTest(properties = {
    "spring.sql.init.schema-locations=classpath:org/springframework/batch/core/schema-h2.sql",
    "spring.jpa.hibernate.ddl-auto=none"
})
class LogUploadBatchIntegrationTest {

  @TempDir
  static Path tempDir;

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private JobRepositoryTestUtils jobRepositoryTestUtils;

  @Autowired
  private Job logUploadJob;

  @MockitoBean
  private S3Client s3Client;

  @MockitoBean
  private AwsProperties awsProperties;

  private static final String TARGET_DATE = "2025-04-28";

  @TestConfiguration
  static class TestLogReaderConfig {

    @Bean("testLogFileItemReader")
    @Primary
    @StepScope
    public LogFileItemReader logFileItemReader(
        @Value("#{jobParameters['targetDate']}") String targetDate) {
      return new LogFileItemReader(targetDate, tempDir.toFile());
    }
  }

  @AfterEach
  void cleanUp() throws IOException {
    try (var stream = Files.list(tempDir)) {
      for (Path file : stream.toList()) {
        Files.deleteIfExists(file);
      }
    }
  }

  @BeforeEach
  void setUp() {
    jobRepositoryTestUtils.removeJobExecutions();
    jobLauncherTestUtils.setJob(logUploadJob);
    given(awsProperties.getBucket()).willReturn("test-bucket");
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willReturn(PutObjectResponse.builder().build());
  }

  @Test
  @DisplayName("로그 파일이 있으면 Job이 COMPLETED 상태로 완료된다")
  void logUploadJob_CompletesWithCompleted_WhenLogFilesExist() throws Exception {
    // given
    // tempDir에 대상 날짜 로그 파일 생성 (TestLogReaderConfig가 tempDir을 바라보도록 설정되어 있음)
    Files.createFile(tempDir.resolve("monew." + TARGET_DATE + ".0.log"));
    JobParameters params = new JobParametersBuilder()
        .addString("targetDate", TARGET_DATE)
        .toJobParameters();

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(params);

    // then
    // Job이 정상 완료되고, S3 업로드가 최소 1회 이상 호출되었는지 확인
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    then(s3Client).should(atLeastOnce())
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("로그 파일이 없으면 Job이 COMPLETED 상태로 완료되고 S3 업로드는 호출되지 않는다")
  void logUploadJob_CompletesWithCompleted_WhenNoLogFilesExist() throws Exception {
    // given
    // 존재하지 않는 날짜로 파라미터 설정 → Reader가 읽을 파일 없음
    // Chunk 읽기 시작 시 즉시 null 반환 → Step이 아무 작업 없이 완료됨
    JobParameters params = new JobParametersBuilder()
        .addString("targetDate", "1999-01-01")
        .toJobParameters();

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(params);

    // then
    // 처리할 파일이 없으므로 S3 업로드 없이 COMPLETED 상태로 종료되어야 함
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    then(s3Client).should(never())
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("S3 업로드 실패 시 Job이 FAILED 상태로 종료된다")
  void logUploadJob_CompletesWithFailed_WhenS3UploadFails() throws Exception {
    // given
    // 로그 파일은 존재하지만 S3 클라이언트가 예외를 던지도록 설정
    // Writer에서 예외 발생 → Step 실패 → Job FAILED 처리
    Files.createFile(tempDir.resolve("monew." + TARGET_DATE + ".0.log"));
    willThrow(new RuntimeException("S3 연결 실패"))
        .given(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    JobParameters params = new JobParametersBuilder()
        .addString("targetDate", TARGET_DATE)
        .toJobParameters();

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(params);

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
  }

  @Test
  @DisplayName("업로드 성공 후 로컬 파일이 삭제된다")
  void logUploadJob_DeletesLocalFiles_WhenUploadSucceeds() throws Exception {
    // given
    // S3 업로드 성공 후 로컬 파일이 정리되는지 전체 흐름을 검증
    // 디스크 용량 낭비 방지를 위해 업로드 완료 파일은 반드시 로컬에서 삭제되어야 함
    File logFile = Files.createFile(tempDir.resolve("monew." + TARGET_DATE + ".1.log")).toFile();
    JobParameters params = new JobParametersBuilder()
        .addString("targetDate", TARGET_DATE)
        .toJobParameters();

    // when
    jobLauncherTestUtils.launchJob(params);

    // then
    assertThat(logFile).doesNotExist();
  }
}
