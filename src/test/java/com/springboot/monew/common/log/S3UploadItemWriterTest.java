package com.springboot.monew.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import com.springboot.monew.newsarticles.s3.AwsProperties;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class S3UploadItemWriterTest {

  @TempDir
  Path tempDir;

  @Mock
  private S3Client s3Client;

  @Mock
  private AwsProperties props;

  private S3UploadItemWriter writer;

  @BeforeEach
  void setUp() {
    lenient().when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());
    given(props.getBucket()).willReturn("test-bucket");
    writer = new S3UploadItemWriter(s3Client, props);
  }

  @Test
  @DisplayName("S3 업로드 성공 후 로컬 파일이 삭제된다")
  void write_DeletesLocalFiles_WhenUploadSucceeds() throws Exception {
    // given
    File logFile = Files.createFile(tempDir.resolve("monew.2025-04-28.0.log")).toFile();
    Chunk<File> chunk = new Chunk<>(List.of(logFile));

    // when
    writer.write(chunk);

    // then
    // S3 업로드가 1회 호출되었고, 업로드 완료 후 로컬 파일이 삭제되었는지 확인
    then(s3Client).should(times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    assertThat(logFile).doesNotExist();
  }

  @Test
  @DisplayName("여러 파일을 모두 업로드한 후 순서대로 삭제한다")
  void write_UploadsAndDeletesAllFiles_WhenMultipleFilesGiven() throws Exception {
    // given
    // Chunk에 파일 2개 포함 → 업로드 2회, 삭제 2회가 일어나야 함
    // 삭제는 모든 업로드가 완료된 이후에 일괄 처리됨 (업로드 중 실패 시 삭제 방지)
    File file1 = Files.createFile(tempDir.resolve("monew.2025-04-28.0.log")).toFile();
    File file2 = Files.createFile(tempDir.resolve("monew.2025-04-28.1.log")).toFile();
    Chunk<File> chunk = new Chunk<>(List.of(file1, file2));

    // when
    writer.write(chunk);

    // then
    then(s3Client).should(times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    assertThat(file1).doesNotExist();
    assertThat(file2).doesNotExist();
  }

  @Test
  @DisplayName("로컬 파일 삭제 실패 시 IllegalStateException을 던진다")
  @DisabledOnOs(OS.WINDOWS)
  void write_ThrowsIllegalStateException_WhenFileDeletionFails() throws Exception {
    // given
    // Unix 계열에서 파일 삭제는 파일 자체가 아닌 부모 디렉토리의 쓰기 권한이 필요함
    // 부모 디렉토리를 쓰기 금지로 설정하여 삭제 불가 상황 재현
    Path subDir = Files.createDirectory(tempDir.resolve("logs"));
    File logFile = Files.createFile(subDir.resolve("monew.2025-04-28.0.log")).toFile();
    subDir.toFile().setWritable(false);
    Chunk<File> chunk = new Chunk<>(List.of(logFile));

    try {
      // when & then
      // S3 업로드는 성공하지만 이후 로컬 삭제 단계에서 IllegalStateException 발생해야 함
      assertThatThrownBy(() -> writer.write(chunk))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("로컬 파일 삭제 실패");
    } finally {
      subDir.toFile().setWritable(true); // 테스트 후 권한 복구
    }
  }

  @Test
  @DisplayName("S3 업로드 실패 시 로컬 파일이 삭제되지 않는다")
  void write_DoesNotDeleteLocalFiles_WhenS3UploadFails() throws Exception {
    // given
    // 업로드 → 삭제 순서로 처리되므로, 업로드 실패 시 삭제 단계까지 도달하지 않아야 함
    // 로컬 파일은 재시도 가능성을 위해 보존되어야 함
    File logFile = Files.createFile(tempDir.resolve("monew.2025-04-28.0.log")).toFile();
    Chunk<File> chunk = new Chunk<>(List.of(logFile));
    willThrow(new RuntimeException("S3 연결 실패"))
        .given(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

    // when & then
    assertThatThrownBy(() -> writer.write(chunk))
        .isInstanceOf(RuntimeException.class);
    assertThat(logFile).exists(); // 업로드 실패 → 로컬 파일 보존
  }

  @Test
  @DisplayName("S3 키는 logs/ 접두사와 파일명으로 구성된다")
  void write_UsesCorrectS3Key_WhenUploadingFile() throws Exception {
    // given
    // S3에 저장되는 키 형식 검증: "logs/{파일명}" 구조여야 함
    // ArgumentCaptor로 실제 전달된 PutObjectRequest를 캡처하여 검증
    File logFile = Files.createFile(tempDir.resolve("monew.2025-04-28.0.log")).toFile();
    Chunk<File> chunk = new Chunk<>(List.of(logFile));
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

    // when
    writer.write(chunk);

    // then
    then(s3Client).should().putObject(captor.capture(), any(RequestBody.class));
    PutObjectRequest captured = captor.getValue();
    assertThat(captured.key()).isEqualTo("logs/monew.2025-04-28.0.log");
    assertThat(captured.bucket()).isEqualTo("test-bucket");
  }
}
