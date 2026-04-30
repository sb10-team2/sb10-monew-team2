package com.springboot.monew.newsarticles.s3;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class S3BackupService {

  private final S3Client s3Client;
  private final AwsProperties props;

  /*
  S3에 백업 JSON 업로드
  key: S3에 저장될 경로
  json: 업로드할 JSON 문자열
  */
  public void upload(String key, String json) {
    try {
      putObject(key, json, null);
    } catch (Exception e) {
      throw new RuntimeException("S3 upload failed", e);
    }
  }

  /*
  파일이 없을 때만 S3에 백업 JSON 업로드
  key: S3에 저장될 경로
  json: 업로드할 JSON 문자열
  return true: 업로드 성공, false: 이미 파일이 존재함
  */
  public boolean uploadIfAbsent(String key, String json) {
    try {
      putObject(key, json, "*");
      return true;
    } catch (S3Exception e) {
      if (e.statusCode() == 412) {
        return false;
      }
      throw new RuntimeException("S3 업로드 실패", e);
    } catch (Exception e) {
      throw new RuntimeException("S3 업로드 실패", e);
    }
  }

  //실제 업로드 수행
  private void putObject(String key, String json, String ifNoneMatch) {
    PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
        .bucket(props.getBucket())
        .key(key)
        .contentType("application/json");

    if (ifNoneMatch != null) {
      requestBuilder.ifNoneMatch(ifNoneMatch);
    }

    s3Client.putObject(
        requestBuilder.build(),
        RequestBody.fromString(json, StandardCharsets.UTF_8)  //JSON 문자열을 바이트로 변환
    );
  }

  //백업 JSON 다운로드
  //key: S3 저장된 파일 경로
  public String downloadJson(String key) {

    //다운로드 요청 생성
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(props.getBucket())  //대상 버킷
        .key(key)                   //다운로드할 파일 경로
        .build();

    //S3에서 파일을 바이트 형태로 가져옴
    ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);

    //바이트 -> 문자열(JSON)으로 변환 후 반환
    return response.asString(StandardCharsets.UTF_8);
  }

  //S3에 해당 key에 파일이 존재하는지 확인
  //key: 확인할 파일 경로
  public boolean exists(String key) {
    try {

      //파일 메타데이터 조회 요청
      HeadObjectRequest request = HeadObjectRequest.builder()
          .bucket(props.getBucket())
          .key(key)
          .build();

      //존재하면 정상 실행됨
      s3Client.headObject(request);
      return true;

    } catch (NoSuchKeyException e) {
      return false;
    }
  }
}
