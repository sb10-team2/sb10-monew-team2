package com.springboot.monew.newsarticle.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws.s3")  // prefix 변경
public class AwsProperties {

  private String accessKey;
  private String secretKey;
  private String region;
  private String bucket;
  private Long presignedUrlExpiration;

}
