package com.springboot.monew.newsarticles.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cloud.aws.s3")  // prefix 변경
public class AwsProperties {

  private String accessKey;
  private String secretKey;
  private String region;
  private String bucket;
  private Long presignedUrlExpiration;

}
