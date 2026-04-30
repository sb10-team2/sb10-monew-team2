package com.springboot.monew.newsarticles.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.newsarticles.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticles.dto.response.NewsArticleCursorRow;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.enums.NewsArticleDirection;
import com.springboot.monew.newsarticles.enums.NewsArticleOrderBy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NewsArticleRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  //테스트용 NewsArticle 생성 메서드
  //반복되는 객체 생성 줄이기 위해 사용
  private NewsArticle createArticle(String originalLink, Instant publishedAt) {
    return NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink(originalLink)
        .title("test title")
        .publishedAt(publishedAt)
        .summary("test summary")
        .build();
  }

  @Test
  @DisplayName("QueryDSL - 발행일 기준 내림차순 정렬 조회에 성공한다.")
  void findNewsArticles_ReturnsSortedByPublishedAtDesc_WhenOrderByPublishDateDesc() {
    // given
    // 서로 다른 발행일을 가진 뉴스 기사 2개 생성
    Instant now = Instant.now();

    NewsArticle oldArticle = createArticle("old", now.minusSeconds(3600)); // 1시간 전
    NewsArticle newArticle = createArticle("new", now); // 현재

    newsArticleRepository.saveAll(List.of(oldArticle, newArticle));

    // 영속성 컨텍스트 초기화 (DB 기준으로 검증하기 위함)
    flushAndClear();

    // QueryDSL 요청 객체 생성 (발행일 기준 내림차순)
    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,   // keyword
        null,   // interestId
        null,   // sourceIn
        null,   // publishDateFrom
        null,   // publishDateTo
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        null,   // cursor
        null,   // after
        10      // limit
    );

    // when
    // QueryDSL 기반 동적 쿼리 실행
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    // 최신 데이터가 먼저 조회되는지 검증
    assertThat(result)
        .extracting(NewsArticleCursorRow::sourceUrl)
        .containsExactly("new", "old");
  }

  @Test
  @DisplayName("QueryDSL - limit + 1 조회로 다음 페이지 존재 여부를 판단한다.")
  void findNewsArticles_ReturnsLimitPlusOne_WhenMoreDataExists() {

    // given
    // limit보다 많은 데이터 생성
    Instant now = Instant.now();

    newsArticleRepository.saveAll(List.of(
        createArticle("a", now.minusSeconds(1)),
        createArticle("b", now.minusSeconds(2)),
        createArticle("c", now.minusSeconds(3))
    ));

    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        null,
        null,
        2 // limit = 2
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    // limit + 1 = 3개 조회되는지 확인
    // → hasNext 판단을 위한 핵심 로직 검증
    assertThat(result).hasSize(3);
  }

  @Test
  @DisplayName("QueryDSL - 뉴스기사 전체 개수를 조회할 수 있다.")
  void countNewsArticles_ReturnsTotalCount_WhenArticlesExist() {

    // given
    // 뉴스기사 3개 저장
    newsArticleRepository.saveAll(List.of(
        createArticle("a", Instant.now()),
        createArticle("b", Instant.now()),
        createArticle("c", Instant.now())
    ));

    flushAndClear();

    // 조건 없는 요청 → 전체 조회
    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        null,
        null,
        10
    );

    // when
    long count = newsArticleRepository.countNewsArticles(request);

    // then
    // 전체 데이터 개수 반환 검증
    assertThat(count).isEqualTo(3L);
  }

  @Test
  @DisplayName("원본링크 목록으로 뉴스기사 조회에 성공한다.")
  void findAllByOriginalLinkIn_ReturnsNewsArticles_WhenOriginalLinksExist() {
    //  given
    // 여러개의 뉴스기사 저장
    NewsArticle article1 = createArticle("link1", Instant.now());
    NewsArticle article2 = createArticle("link2", Instant.now());
    NewsArticle article3 = createArticle("link3", Instant.now());

    newsArticleRepository.saveAll(List.of(article1, article2, article3));

    //  when
    // 특정 originalLink 목록으로 조회
    List<NewsArticle> result = newsArticleRepository.findAllByOriginalLinkIn(List.of("link1", "link3"));

    //  then
    // 조회도니 결과가 2개인지 검증
    assertThat(result).hasSize(2);

    // 조회된 결과의 originalLink 값이 기대값과 일치하는지 검증
    assertThat(result)
        .extracting(NewsArticle::getOriginalLink)
        .containsExactlyInAnyOrder("link1", "link3");
  }

  @Test
  @DisplayName("원본링크 목록에 해당하는 뉴스기사가 없으면 빈 목록을 반환한다.")
  void findAllByOriginalLinkIn_ReturnsEmptyList_WhenOriginalLinksDoNotExist() {
    // given
    NewsArticle article = createArticle("link1", Instant.now());
    newsArticleRepository.save(article);

    // when
    List<NewsArticle> result =
        newsArticleRepository.findAllByOriginalLinkIn(List.of("not-exist"));

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("뉴스기사 조회수 증가에 성공한다.")
  void incrementViewCount_IncreasesViewCount_WhenArticleExists() {
    //  given
    // 조회수 0인 기사 저장
    NewsArticle article = createArticle("link", Instant.now());
    NewsArticle saved = newsArticleRepository.save(article);

    //  when
    // JPQL update 쿼리로 조회수 증가
    newsArticleRepository.incrementViewCount(saved.getId());

    //  then
    // DB에 반영된값을 다시 조회하여 검증
    NewsArticle updated = newsArticleRepository.findById(saved.getId()).orElseThrow();

    // 조회수가 1 증가했는지 확인
    assertThat(updated.getViewCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("존재하지 않는 뉴스기사 조회수 증가는 영향을 주지 않는다.")
  void incrementViewCount_DoesNothing_WhenArticleDoesNotExist() {
    // given
    // DB에 존재하지 않는 임의의 ID 생성
    UUID notExistArticleId = UUID.randomUUID();

    // when
    // 존재하지 않는 ID로 조회수 증가 쿼리 실행
    newsArticleRepository.incrementViewCount(notExistArticleId);

    // 영속성 컨텍스트를 DB와 동기화하고 1차 캐시를 비움
    flushAndClear();

    // then
    // 해당 ID로 조회 시 아무 데이터도 없어야 함
    assertThat(newsArticleRepository.findById(notExistArticleId)).isEmpty();
  }

  @Test
  @DisplayName("기간 조건으로 뉴스기사 조회에 성공한다(날짜 기준)")
  void findAllByPublishedAtGreaterThanEqualAndPublishedAtLessThan_ReturnsArticles_WhenDateFilterApplied() {
    // given
    LocalDate today = LocalDate.now();

    // 날짜 기준으로 시작/끝 Instant 생성
    Instant startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
    Instant startOfTomorrow = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

    // 어제 / 오늘 / 내일 데이터 생성
    NewsArticle yesterday = createArticle("yesterday",
        today.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

    NewsArticle todayArticle = createArticle("today",
        startOfToday.plusSeconds(3600)); // 오늘 1시간 후

    NewsArticle tomorrow = createArticle("tomorrow",
        startOfTomorrow.plusSeconds(3600));

    newsArticleRepository.saveAll(List.of(yesterday, todayArticle, tomorrow));

    // when
    List<NewsArticle> result =
        newsArticleRepository.findAllByPublishedAtGreaterThanEqualAndPublishedAtLessThan(
            startOfToday,
            startOfTomorrow
        );

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOriginalLink()).isEqualTo("today");
  }

}
