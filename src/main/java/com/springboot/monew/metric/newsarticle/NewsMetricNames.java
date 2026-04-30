package com.springboot.monew.metric.newsarticle;

// 뉴스 기사 수집과 백업 스케줄러 도메인 메트릭 이름 모음
public final class NewsMetricNames {

  // 뉴스 source별 수집 실행 횟수 
  public static final String NEWS_COLLECT_SOURCE_RUNS = "monew.news.collect.source.runs";

  // 뉴스 source별 수집 실행 시간 
  public static final String NEWS_COLLECT_SOURCE_DURATION = "monew.news.collect.source.duration";

  // 외부 source에서 가져온 기사 수 
  public static final String NEWS_COLLECT_COLLECTED_ARTICLES = "monew.news.collect.collected.articles";

  // 관심사 키워드와 매칭된 기사 수 
  public static final String NEWS_COLLECT_MATCHED_ARTICLES = "monew.news.collect.matched.articles";

  // DB에 새로 저장된 기사 수 
  public static final String NEWS_COLLECT_SAVED_ARTICLES = "monew.news.collect.saved.articles";

  // 중복으로 신규 저장 대상에서 제외된 기사 수 
  public static final String NEWS_COLLECT_DUPLICATE_ARTICLES = "monew.news.collect.duplicate.articles";

  // 새로 저장된 기사와 관심사 연결 수 
  public static final String NEWS_COLLECT_SAVED_ARTICLE_INTERESTS = "monew.news.collect.saved.article.interests";

  // 마지막 뉴스 수집 실행의 관심사 키워드 수 
  public static final String NEWS_COLLECT_LAST_KEYWORD_COUNT = "monew.news.collect.last.keyword.count";

  // 마지막 뉴스 수집 실행의 처리 source 수 
  public static final String NEWS_COLLECT_LAST_SOURCE_COUNT = "monew.news.collect.last.source.count";

  // 마지막 뉴스 수집 실행의 성공 source 수 
  public static final String NEWS_COLLECT_LAST_SUCCESS_SOURCE_COUNT = "monew.news.collect.last.success.source.count";

  // 마지막 뉴스 수집 실행의 외부 수집 기사 수 
  public static final String NEWS_COLLECT_LAST_COLLECTED_COUNT = "monew.news.collect.last.collected.count";

  // 마지막 뉴스 수집 실행의 관심사 매칭 기사 수 
  public static final String NEWS_COLLECT_LAST_MATCHED_COUNT = "monew.news.collect.last.matched.count";

  // 마지막 뉴스 수집 실행의 신규 저장 기사 수 
  public static final String NEWS_COLLECT_LAST_SAVED_COUNT = "monew.news.collect.last.saved.count";

  // 마지막 뉴스 수집 실행의 중복 제외 기사 수 
  public static final String NEWS_COLLECT_LAST_DUPLICATE_COUNT = "monew.news.collect.last.duplicate.count";

  // 마지막 뉴스 수집 실행의 신규 기사-관심사 연결 수 
  public static final String NEWS_COLLECT_LAST_SAVED_ARTICLE_INTEREST_COUNT = "monew.news.collect.last.saved.article.interest.count";

  // 마지막 뉴스 수집 실행의 실패 source 수 
  public static final String NEWS_COLLECT_LAST_FAILED_SOURCE_COUNT = "monew.news.collect.last.failed.source.count";

  // 뉴스 백업 작업에서 확인한 날짜 수 
  public static final String NEWS_BACKUP_CHECKED_DATES = "monew.news.backup.checked.dates";

  // 뉴스 백업 작업에서 새로 업로드한 파일 수 
  public static final String NEWS_BACKUP_UPLOADED_FILES = "monew.news.backup.uploaded.files";

  // 뉴스 백업 작업에서 기존 파일로 스킵한 파일 수 
  public static final String NEWS_BACKUP_SKIPPED_FILES = "monew.news.backup.skipped.files";

  // 뉴스 백업 작업에서 실패한 날짜 수 
  public static final String NEWS_BACKUP_FAILED_FILES = "monew.news.backup.failed.files";

  // 새로 업로드한 백업 파일에 포함된 기사 수 
  public static final String NEWS_BACKUP_ARTICLES = "monew.news.backup.articles";

  // 새로 업로드한 백업 payload byte 크기 
  public static final String NEWS_BACKUP_PAYLOAD_BYTES = "monew.news.backup.payload.bytes";

  // 마지막 뉴스 백업 실행의 업로드 파일 수 
  public static final String NEWS_BACKUP_LAST_UPLOADED_COUNT = "monew.news.backup.last.uploaded.count";

  // 마지막 뉴스 백업 실행의 확인 날짜 수 
  public static final String NEWS_BACKUP_LAST_CHECKED_DATE_COUNT = "monew.news.backup.last.checked.date.count";

  // 마지막 뉴스 백업 실행의 스킵 파일 수 
  public static final String NEWS_BACKUP_LAST_SKIPPED_COUNT = "monew.news.backup.last.skipped.count";

  // 마지막 뉴스 백업 실행의 실패 날짜 수 
  public static final String NEWS_BACKUP_LAST_FAILED_COUNT = "monew.news.backup.last.failed.count";

  // 마지막 뉴스 백업 실행의 백업 기사 수 
  public static final String NEWS_BACKUP_LAST_ARTICLE_COUNT = "monew.news.backup.last.article.count";

  // 마지막 뉴스 백업 실행의 업로드 payload byte 크기 
  public static final String NEWS_BACKUP_LAST_PAYLOAD_BYTES = "monew.news.backup.last.payload.bytes";

  private NewsMetricNames() {
  }
}
