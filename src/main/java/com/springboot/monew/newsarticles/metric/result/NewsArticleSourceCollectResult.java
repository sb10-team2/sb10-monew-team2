package com.springboot.monew.newsarticles.metric.result;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import java.time.Duration;

// 뉴스 기사 source 하나의 수집과 저장 처리 결과 값 객체
public record NewsArticleSourceCollectResult(
    // 수집 대상 뉴스 source
    ArticleSource source,

    // 외부 source에서 가져온 기사 수
    int collectedArticleCount,

    // 관심사 키워드와 매칭된 기사 수
    int matchedArticleCount,

    // DB 저장 처리 결과
    NewsArticleSaveResult saveResult,

    // source 처리 성공 여부
    boolean success,

    // source 처리에 걸린 시간
    Duration duration
) {

  // 음수 집계 값과 null 저장 결과 및 비정상 Duration 보정
  public NewsArticleSourceCollectResult {
    collectedArticleCount = Math.max(0, collectedArticleCount);
    matchedArticleCount = Math.max(0, matchedArticleCount);
    saveResult = saveResult == null ? NewsArticleSaveResult.empty() : saveResult;
    duration = duration == null || duration.isNegative() ? Duration.ZERO : duration;
  }

  // source 처리 성공 결과 생성
  public static NewsArticleSourceCollectResult success(ArticleSource source,
      int collectedArticleCount,
      int matchedArticleCount, NewsArticleSaveResult saveResult, Duration duration) {
    return new NewsArticleSourceCollectResult(source, collectedArticleCount, matchedArticleCount,
        saveResult, true, duration);
  }

  // source 처리 실패 결과 생성
  public static NewsArticleSourceCollectResult failure(ArticleSource source, Duration duration) {
    return new NewsArticleSourceCollectResult(source, 0, 0, NewsArticleSaveResult.empty(), false,
        duration);
  }

  // DB에 새로 저장된 기사 수
  public int savedArticleCount() {
    return saveResult.savedArticleCount();
  }

  // 중복으로 제외된 기사 수
  public int duplicateArticleCount() {
    return saveResult.duplicateArticleCount();
  }

  // 새로 저장된 기사-관심사 연결 수
  public int savedArticleInterestCount() {
    return saveResult.savedArticleInterestCount();
  }
}
