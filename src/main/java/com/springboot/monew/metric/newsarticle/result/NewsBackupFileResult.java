package com.springboot.monew.metric.newsarticle.result;

// 날짜 하나에 대한 뉴스 기사 백업 파일 처리 결과 값 객체
public record NewsBackupFileResult(
    // S3에 새 백업 파일을 업로드했는지 여부
    boolean uploaded,

    // 기존 백업 파일로 업로드를 스킵했는지 여부
    boolean skipped,

    // 새로 업로드한 백업 파일에 포함된 기사 수
    int articleCount,

    // 새로 업로드한 JSON payload byte 크기
    long payloadBytes
) {

  // 음수 기사 수와 payload 크기 보정
  public NewsBackupFileResult {
    articleCount = Math.max(0, articleCount);
    payloadBytes = Math.max(0L, payloadBytes);
  }

  // 새 백업 파일 업로드 결과 생성
  public static NewsBackupFileResult uploaded(int articleCount, long payloadBytes) {
    return new NewsBackupFileResult(true, false, articleCount, payloadBytes);
  }

  // 기존 백업 파일로 인한 스킵 결과 생성
  public static NewsBackupFileResult skippedByExistingFile() {
    return new NewsBackupFileResult(false, true, 0, 0L);
  }
}
