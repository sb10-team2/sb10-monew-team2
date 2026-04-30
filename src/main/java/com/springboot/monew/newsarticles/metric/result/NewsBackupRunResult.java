package com.springboot.monew.newsarticles.metric.result;

import com.springboot.monew.common.metric.ScheduledTaskStatus;
import java.util.List;
import java.util.Objects;

// 뉴스 백업 스케줄러 전체 실행 결과 값 객체
public record NewsBackupRunResult(
    // 백업 여부를 확인한 날짜 수
    long checkedDateCount,

    // 날짜별 백업 파일 처리 결과 목록
    List<NewsBackupFileResult> fileResults,

    // 백업 처리에 실패한 날짜 수
    long failedCount
) {

  // 음수 집계 값과 null 파일 결과 목록 보정
  public NewsBackupRunResult {
    checkedDateCount = Math.max(0L, checkedDateCount);
    fileResults = fileResults == null
        ? List.of()
        : fileResults.stream()
            .filter(Objects::nonNull)
            .toList();
    failedCount = Math.max(0L, failedCount);
  }

  // 새로 업로드한 백업 파일 수
  public long uploadedFileCount() {
    return fileResults.stream()
        .filter(NewsBackupFileResult::uploaded)
        .count();
  }

  // 기존 파일로 스킵한 백업 파일 수
  public long skippedFileCount() {
    return fileResults.stream()
        .filter(NewsBackupFileResult::skipped)
        .count();
  }

  // 새로 업로드한 백업 파일에 포함된 기사 수 합계
  public long totalArticleCount() {
    return fileResults.stream()
        .filter(NewsBackupFileResult::uploaded)
        .mapToLong(NewsBackupFileResult::articleCount)
        .sum();
  }

  // 새로 업로드한 백업 payload byte 크기 합계
  public long totalPayloadBytes() {
    return fileResults.stream()
        .filter(NewsBackupFileResult::uploaded)
        .mapToLong(NewsBackupFileResult::payloadBytes)
        .sum();
  }

  // 뉴스 백업 결과 기반 공통 스케줄 상태
  public ScheduledTaskStatus scheduledTaskStatus() {
    if (checkedDateCount == 0L || failedCount >= checkedDateCount) {
      return ScheduledTaskStatus.FAILURE;
    }

    if (failedCount > 0L) {
      return ScheduledTaskStatus.PARTIAL_FAILURE;
    }

    return ScheduledTaskStatus.SUCCESS;
  }
}
