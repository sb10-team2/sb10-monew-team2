package com.springboot.monew.common.log;

import com.springboot.monew.newsarticle.s3.AwsProperties;
import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@RequiredArgsConstructor
public class S3UploadItemWriter implements ItemWriter<File> {

  private final S3Client s3Client;
  private final AwsProperties props;

  @Override
  public void write(Chunk<? extends File> logFiles) throws Exception {
    for (File logFile : logFiles) {
      String key = buildS3Key(logFile.getName());
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(props.getBucket())
          .key(key)
          .contentType("text/plain")
          .build();
      s3Client.putObject(request, RequestBody.fromFile(logFile));
      log.info("S3 업로드 완료: {}", key);
    }

    // 업로드 후에 로컬에서 로그 파일 삭제
    for (File logFile : logFiles) {
      if (logFile.delete()) {
        log.info("로컬 파일 삭제 완료: {}", logFile.getName());
      } else {
        throw new IllegalStateException("로컬 파일 삭제 실패: " + logFile.getName());
      }
    }
  }

  private String buildS3Key(String fileName) {
    return "logs/" + fileName;
  }
}
