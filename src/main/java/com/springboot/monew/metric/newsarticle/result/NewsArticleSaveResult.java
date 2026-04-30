package com.springboot.monew.metric.newsarticle.result;

// 뉴스 기사 저장 결과를 메트릭 집계에 재사용하기 위한 값 객체
public record NewsArticleSaveResult(
    // 저장 요청으로 전달된 기사 수
    int requestedArticleCount,

    // originalLink 기준 요청 내 중복 제거 후 기사 수
    int distinctArticleCount,

    // DB에 이미 존재해서 신규 저장하지 않은 기사 수
    int existingArticleCount,

    // DB에 새로 저장한 기사 수
    int savedArticleCount,

    // 새로 저장한 기사-관심사 연결 수
    int savedArticleInterestCount
) {

  // 음수 저장 집계 값 보정
  public NewsArticleSaveResult {
    requestedArticleCount = Math.max(0, requestedArticleCount);
    distinctArticleCount = Math.max(0, distinctArticleCount);
    existingArticleCount = Math.max(0, existingArticleCount);
    savedArticleCount = Math.max(0, savedArticleCount);
    savedArticleInterestCount = Math.max(0, savedArticleInterestCount);
  }

  // 저장 대상이 없을 때 사용하는 빈 저장 결과 생성
  public static NewsArticleSaveResult empty() {
    return new NewsArticleSaveResult(0, 0, 0, 0, 0);
  }

  // 요청 내 중복과 DB 기존 데이터 기반 중복 제외 수 계산
  public int duplicateArticleCount() {
    int duplicatedInRequestCount = Math.max(0, requestedArticleCount - distinctArticleCount);
    return duplicatedInRequestCount + existingArticleCount;
  }
}
