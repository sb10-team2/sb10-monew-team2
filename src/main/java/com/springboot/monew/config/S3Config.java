package com.springboot.monew.config;

import com.springboot.monew.newsarticles.s3.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class S3Config {
  private final AwsProperties props;

  public S3Config(AwsProperties props) {this.props = props;}

  @Bean
  public S3Client s3Client() {//S3Client: S3 API 호출용 클라이언트(S3에 요청 보내는 객체)

    log.info("==== S3 CONFIG DEBUG ====");
    log.info("accessKey: {}", props.getAccessKey());
    log.info("secretKey: {}", props.getSecretKey());
    log.info("region: {}", props.getRegion());
    log.info("bucket: {}", props.getBucket());
    log.info("=========================");

    String region = props.getRegion();

    if (region == null || region.isBlank()) {
      throw new IllegalStateException("AWS_S3_REGION 설정이 누락되었습니다.");
    }

    // 키가 있는경우
    boolean hasAccessKey = props.getAccessKey() != null && !props.getAccessKey().isBlank();
    boolean hasSecretKey = props.getSecretKey() != null && !props.getSecretKey().isBlank();
    if (hasAccessKey || hasSecretKey) {
      if(!(hasAccessKey && hasSecretKey)) {
        throw new IllegalArgumentException("S3 access key와 secret key는 함께 설정되어야 합니다.");
      }
      return S3Client.builder()
          .region(Region.of(props.getRegion()))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(
                      props.getAccessKey(),
                      props.getSecretKey()
                  )
              )
          )
          .build();
    }

    // 그렇지 않으면: 기본 체인(환경변수, 프로파일, IAM Role)을 자동 탐색
    // 키가 없는경우
    return S3Client.builder()
        .region(Region.of(props.getRegion()))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }

}
