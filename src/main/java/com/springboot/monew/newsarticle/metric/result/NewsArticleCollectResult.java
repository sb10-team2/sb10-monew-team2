package com.springboot.monew.newsarticle.metric.result;

import com.springboot.monew.common.metric.ScheduledTaskStatus;
import java.util.List;
import java.util.Objects;

// 뉴스 수집 스케줄러 전체 실행 결과 값 객체
public record NewsArticleCollectResult(
    // 이번 수집 실행에서 사용한 관심사 키워드 수
    int keywordCount,

    // source별 뉴스 수집 결과 목록
    List<NewsArticleSourceCollectResult> sourceResults
) {

  // 음수 키워드 수와 null source 결과 목록 보정
  public NewsArticleCollectResult {
    keywordCount = Math.max(0, keywordCount);
    sourceResults = sourceResults == null
        ? List.of()
        : sourceResults.stream()
            .filter(Objects::nonNull)
            .toList();
  }

  // 모든 source에서 가져온 기사 수 합계
  public long totalCollectedArticleCount() {
    return sourceResults.stream()
        .mapToLong(NewsArticleSourceCollectResult::collectedArticleCount)
        .sum();
  }

  // 관심사 키워드와 매칭된 기사 수 합계
  public long totalMatchedArticleCount() {
    return sourceResults.stream()
        .mapToLong(NewsArticleSourceCollectResult::matchedArticleCount)
        .sum();
  }

  // DB에 새로 저장된 기사 수 합계
  public long totalSavedArticleCount() {
    return sourceResults.stream()
        .mapToLong(NewsArticleSourceCollectResult::savedArticleCount)
        .sum();
  }

  // 중복으로 제외된 기사 수 합계
  public long totalDuplicateArticleCount() {
    return sourceResults.stream()
        .mapToLong(NewsArticleSourceCollectResult::duplicateArticleCount)
        .sum();
  }

  // 새로 저장된 기사-관심사 연결 수 합계
  public long totalSavedArticleInterestCount() {
    return sourceResults.stream()
        .mapToLong(NewsArticleSourceCollectResult::savedArticleInterestCount)
        .sum();
  }

  // 처리 대상 source 수
  public long sourceCount() {
    return sourceResults.size();
  }

  // 성공한 source 수
  public long successSourceCount() {
    return sourceResults.stream()
        .filter(NewsArticleSourceCollectResult::success)
        .count();
  }

  // 실패한 source 수
  public long failedSourceCount() {
    return sourceResults.stream()
        .filter(result -> !result.success())
        .count();
  }

  // 실패 source 존재 여부
  public boolean hasFailure() {
    return failedSourceCount() > 0L;
  }

  // 모든 source 실패 여부
  public boolean allSourcesFailed() {
    return !sourceResults.isEmpty() && failedSourceCount() == sourceResults.size();
  }

  // 뉴스 수집 결과 기반 공통 스케줄 상태
  public ScheduledTaskStatus scheduledTaskStatus() {
    if (sourceResults.isEmpty() || allSourcesFailed()) {
      return ScheduledTaskStatus.FAILURE;
    }

    if (hasFailure()) {
      return ScheduledTaskStatus.PARTIAL_FAILURE;
    }

    return ScheduledTaskStatus.SUCCESS;
  }
}
