package com.springboot.monew.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogFileItemReaderTest {

  @TempDir
  Path tempDir;

  private static final String TARGET_DATE = "2025-04-28";

  @Test
  @DisplayName("해당 날짜 로그 파일만 필터링하여 읽어온다")
  void read_ReturnsOnlyMatchingDateFiles_WhenMultipleFilesExist() throws Exception {
    // given
    // 대상 날짜 파일 2개와 다른 날짜 파일 1개를 함께 생성
    // LogFileItemReader는 생성자 시점에 날짜 기준으로 파일 목록을 필터링함
    Files.createFile(tempDir.resolve("monew.2025-04-28.0.log"));
    Files.createFile(tempDir.resolve("monew.2025-04-28.1.log"));
    Files.createFile(tempDir.resolve("monew.2025-04-27.0.log")); // 다른 날짜 → 필터링 대상

    LogFileItemReader reader = new LogFileItemReader(TARGET_DATE, tempDir.toFile());

    // when
    // read()는 내부 인덱스를 순차적으로 증가시키며 파일을 반환하고, 목록 소진 시 null 반환
    File first = reader.read();
    File second = reader.read();
    File third = reader.read(); // 대상 날짜 파일이 2개뿐이므로 null이어야 함

    // then
    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(third).isNull(); // 더 이상 읽을 파일 없음
    assertThat(first.getName()).contains(TARGET_DATE);
    assertThat(second.getName()).contains(TARGET_DATE);
  }

  @Test
  @DisplayName("해당 날짜 로그 파일이 없으면 즉시 null을 반환한다")
  void read_ReturnsNull_WhenNoMatchingFilesExist() throws Exception {
    // given
    // 디렉토리에는 파일이 있지만 대상 날짜(2025-04-28)와 다른 날짜 파일만 존재
    Files.createFile(tempDir.resolve("monew.2025-04-27.0.log"));

    LogFileItemReader reader = new LogFileItemReader(TARGET_DATE, tempDir.toFile());

    // when
    File result = reader.read();

    // then
    // 필터 조건을 만족하는 파일이 없으므로 첫 read() 호출부터 null 반환
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("로그 디렉토리가 없으면 즉시 null을 반환한다")
  void read_ReturnsNull_WhenLogDirectoryDoesNotExist() {
    // given
    // 실제로 존재하지 않는 디렉토리 경로로 Reader 생성
    // listFiles()가 null을 반환하므로 파일 목록이 비어있는 상태로 초기화됨
    File nonExistentDir = tempDir.resolve("non-existent").toFile();
    LogFileItemReader reader = new LogFileItemReader(TARGET_DATE, nonExistentDir);

    // when
    File result = reader.read();

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName(".log 확장자가 아닌 파일은 필터링된다")
  void read_ReturnsNull_WhenFileIsNotLogExtension() throws Exception {
    // given
    // 날짜는 일치하지만 확장자가 .log가 아닌 파일들 (.txt, .log.gz)
    // LogFileItemReader 필터 조건: 파일명에 날짜 포함 AND .log로 끝나야 함
    Files.createFile(tempDir.resolve("monew.2025-04-28.0.txt"));
    Files.createFile(tempDir.resolve("monew.2025-04-28.0.log.gz")); // .log로 끝나지 않음

    LogFileItemReader reader = new LogFileItemReader(TARGET_DATE, tempDir.toFile());

    // when
    File result = reader.read();

    // then
    // 확장자 조건 불일치로 모두 필터링되어 읽을 파일 없음
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("read()를 반복 호출하면 파일을 순서대로 반환하고 마지막에 null을 반환한다")
  void read_ReturnsFilesSequentiallyThenNull_WhenCalledRepeatedly() throws Exception {
    // given
    // 파일 1개만 존재 → 첫 번째 read()는 파일 반환, 두 번째 read()는 null 반환해야 함
    Files.createFile(tempDir.resolve("monew.2025-04-28.0.log"));

    LogFileItemReader reader = new LogFileItemReader(TARGET_DATE, tempDir.toFile());

    // when & then
    // Spring Batch는 ItemReader.read()가 null을 반환할 때 Chunk 읽기를 종료함
    assertThat(reader.read()).isNotNull();
    assertThat(reader.read()).isNull();
  }
}
