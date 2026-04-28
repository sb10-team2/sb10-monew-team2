package com.springboot.monew.config;

import com.springboot.monew.newsarticles.s3.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j

//Spring 설정 클래스
//내부의 @Bean 메서드들을 Bean으로 등록
@Configuration

//yml값을 객체로 바인딩
@ConditionalOnProperty(
    name = "cloud.aws.s3.enabled",  // 이 yml 키를 확인해
    havingValue = "true",           // 값이 "true"일 때만
    matchIfMissing = false          // 키가 없으면 → false 취급 (Bean 안 만듦)
)

//@ConfigurationProperties 클래스(AwsProperties)를 Bean으로 등록.
@EnableConfigurationProperties(AwsProperties.class)
public class S3Config {
  private final AwsProperties props;

  public S3Config(AwsProperties props) {this.props = props;}

  @Bean
  public S3Client s3Client() {//S3Client: S3 API 호출용 클라이언트(S3에 요청 보내는 객체)

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
