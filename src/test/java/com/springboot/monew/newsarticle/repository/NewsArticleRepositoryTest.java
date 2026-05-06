package com.springboot.monew.newsarticle.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.newsarticle.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticle.dto.response.NewsArticleCursorRow;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import com.springboot.monew.newsarticle.enums.ArticleSource;
import com.springboot.monew.newsarticle.enums.NewsArticleDirection;
import com.springboot.monew.newsarticle.enums.NewsArticleOrderBy;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;

public class NewsArticleRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private UserRepository userRepository;

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

  private Comment createComment(NewsArticle article, User user) {
    return new Comment(
        user,
        article,
        "test comment"
    );
  }

  private User createUser() {
    String uniqueValue = UUID.randomUUID().toString();

    return new User(
        uniqueValue + "@test.com",
        "user-" + uniqueValue.substring(0, 8),
        "password123!"
    );
  }

  @Test
  @DisplayName("QueryDSL - 발행일 범위 조건으로 뉴스기사를 필터링한다.")
  void findNewsArticles_ReturnsArticles_WhenPublishDateRangeApplied() {
    // given

    NewsArticle before = createArticle("before", Instant.parse("2026-04-30T00:00:00Z").minusSeconds(1));
    NewsArticle inRange = createArticle("in-range", Instant.parse("2026-04-30T00:00:00Z").plusSeconds(3600));
    NewsArticle after = createArticle("after", Instant.parse("2026-04-30T00:00:00Z").plusSeconds(86400));

    newsArticleRepository.saveAll(List.of(before, inRange, after));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        Instant.parse("2026-04-30T00:00:00Z"),
        Instant.parse("2026-04-30T00:00:00Z").plusSeconds(86399),
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).sourceUrl()).isEqualTo("in-range");
  }

  @Test
  @DisplayName("QueryDSL - 댓글 수 기준 오름차순 정렬 조건으로 조회한다.")
  void findNewsArticles_ReturnsArticles_WhenOrderByCommentCountAsc() {
    // given
    // 서로다른 댓글 수를 가지도록 기사 2개 생성
    NewsArticle first = createArticle("first", Instant.now());
    NewsArticle second = createArticle("second", Instant.now().minusSeconds(10));

    newsArticleRepository.saveAll(List.of(first, second));

    //댓글 저장을 위한 user객체
    User user = userRepository.save(createUser());

    // first: 댓글 2개
    commentRepository.save(createComment(first, user));
    commentRepository.save(createComment(first, user));

    // second: 댓글 1개
    commentRepository.save(createComment(second, user));
    flushAndClear();

    //정렬기준: 댓글수, 오름차순
    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.commentCount,
        NewsArticleDirection.ASC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(2);
    //second(1개) -> first(2개) 순
    assertThat(result.get(0).id()).isEqualTo(second.getId());
    assertThat(result.get(1).id()).isEqualTo(first.getId());
  }

  @Test
  @DisplayName("QueryDSL - 댓글 수 기준 내림차순 정렬 조건으로 조회한다.")
  void findNewsArticles_ReturnsArticles_WhenOrderByCommentCountDesc() {
    // given
    NewsArticle first = createArticle("first", Instant.now());
    NewsArticle second = createArticle("second", Instant.now().minusSeconds(10));

    newsArticleRepository.saveAll(List.of(first, second));

    //댓글 수 저장을 위한 User객체
    User user = userRepository.save(createUser());

    // first: 댓글 2개
    commentRepository.save(createComment(first, user));
    commentRepository.save(createComment(first, user));

    // second: 댓글 1개
    commentRepository.save(createComment(second, user));
    flushAndClear();

    //정렬기준 댓글수, 내림차순
    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.commentCount,
        NewsArticleDirection.DESC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).id()).isEqualTo(first.getId());
    assertThat(result.get(1).id()).isEqualTo(second.getId());
  }

  @Test
  @DisplayName("QueryDSL - 댓글 수 커서 조건으로 다음 페이지를 조회한다.")
  void findNewsArticles_ReturnsArticles_WhenCommentCountCursorExists() {
    // given
    NewsArticle first = createArticle("first", Instant.now());
    NewsArticle second = createArticle("second", Instant.now().minusSeconds(10));

    newsArticleRepository.saveAll(List.of(first, second));
    flushAndClear();

    NewsArticle savedSecond = newsArticleRepository.findById(second.getId()).orElseThrow();

    String cursor = "1|" + savedSecond.getCreatedAt();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.commentCount,
        NewsArticleDirection.DESC,
        cursor,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(2);
  }



  @Test
  @DisplayName("QueryDSL - 발행일 기준 오름차순 정렬 조회에 성공한다.")
  void findNewsArticles_ReturnsSortedByPublishedAtAsc_WhenOrderByPublishDateAsc() {
    // given
    Instant now = Instant.now();

    NewsArticle oldArticle = createArticle("old", now.minusSeconds(3600));
    NewsArticle newArticle = createArticle("new", now);

    newsArticleRepository.saveAll(List.of(newArticle, oldArticle));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.ASC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result)
        .extracting(NewsArticleCursorRow::sourceUrl)
        .containsExactly("old", "new");
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
  @DisplayName("QueryDSL - 검색어가 제목에 포함된 뉴스기사만 조회한다.")
  void findNewsArticles_ReturnsArticles_WhenKeywordMatchesTitle() {
    // given
    NewsArticle springArticle = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("spring-link")
        .title("spring boot news")
        .publishedAt(Instant.now())
        .summary("test summary")
        .build();

    NewsArticle javaArticle = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("java-link")
        .title("java news")
        .publishedAt(Instant.now())
        .summary("test summary")
        .build();

    newsArticleRepository.saveAll(List.of(springArticle, javaArticle));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        "spring",
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
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).sourceUrl()).isEqualTo("spring-link");
  }

  @Test
  @DisplayName("QueryDSL - 출처 조건으로 뉴스기사를 필터링한다.")
  void findNewsArticles_ReturnsArticles_WhenSourceFilterApplied() {
    // given
    NewsArticle naverArticle = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("naver-link")
        .title("naver title")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    NewsArticle yonhapArticle = NewsArticle.builder()
        .source(ArticleSource.YEONHAP)
        .originalLink("yonhap-link")
        .title("yonhap title")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    newsArticleRepository.saveAll(List.of(naverArticle, yonhapArticle));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        List.of(ArticleSource.NAVER),
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).source()).isEqualTo(ArticleSource.NAVER);
  }
  @Test
  @DisplayName("QueryDSL - 출처 조건이 빈 리스트이면 필터링하지 않는다.")
  void findNewsArticles_ReturnsAllArticles_WhenSourceInIsEmpty() {
    // given
    NewsArticle naver = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("naver")
        .title("title")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    NewsArticle yonhap = NewsArticle.builder()
        .source(ArticleSource.YEONHAP)
        .originalLink("yonhap")
        .title("title")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    newsArticleRepository.saveAll(List.of(naver, yonhap));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        List.of(),
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("QueryDSL - 조회수 기준 오름차순 정렬 조회에 성공한다.")
  void findNewsArticles_ReturnsSortedByViewCountAsc_WhenOrderByViewCountAsc() {
    // given
    NewsArticle low = createArticle("low", Instant.now());
    NewsArticle high = createArticle("high", Instant.now());

    newsArticleRepository.saveAll(List.of(low, high));
    flushAndClear();

    newsArticleRepository.incrementViewCount(high.getId());
    newsArticleRepository.incrementViewCount(high.getId());
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.viewCount,
        NewsArticleDirection.ASC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result)
        .extracting(NewsArticleCursorRow::sourceUrl)
        .containsExactly("low", "high");
  }

  @Test
  @DisplayName("QueryDSL - 조회수 기준 내림차순 정렬 조회에 성공한다.")
  void findNewsArticles_ReturnsSortedByViewCountDesc_WhenOrderByViewCountDesc() {
    // given
    NewsArticle low = createArticle("low", Instant.now());
    NewsArticle high = createArticle("high", Instant.now());

    newsArticleRepository.saveAll(List.of(low, high));
    flushAndClear();

    newsArticleRepository.incrementViewCount(high.getId());
    newsArticleRepository.incrementViewCount(high.getId());
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.viewCount,
        NewsArticleDirection.DESC,
        null,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result)
        .extracting(NewsArticleCursorRow::sourceUrl)
        .containsExactly("high", "low");
  }

  @Test
  @DisplayName("QueryDSL - 조회수 커서 조건으로 다음 페이지를 조회한다.")
  void findNewsArticles_ReturnsNextPage_WhenViewCountCursorExists() {
    // given
    NewsArticle low = createArticle("low", Instant.now());
    NewsArticle high = createArticle("high", Instant.now().minusSeconds(10));
    NewsArticle zero = createArticle("zero", Instant.now().minusSeconds(20));

    newsArticleRepository.saveAll(List.of(low, high, zero));
    flushAndClear();

    newsArticleRepository.incrementViewCount(low.getId());
    newsArticleRepository.incrementViewCount(high.getId());
    newsArticleRepository.incrementViewCount(high.getId());
    flushAndClear();

    NewsArticle savedLow = newsArticleRepository.findById(low.getId()).orElseThrow();

    String cursor = "1|" + savedLow.getCreatedAt();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.viewCount,
        NewsArticleDirection.DESC,
        cursor,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result)
        .extracting(NewsArticleCursorRow::sourceUrl)
        .containsExactly("zero");
  }

  @Test
  @DisplayName("QueryDSL - 잘못된 커서 형식이면 예외가 발생한다.")
  void findNewsArticles_ThrowsException_WhenCursorFormatIsInvalid() {
    // given
    NewsArticle article = createArticle("link", Instant.now());
    newsArticleRepository.save(article);
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        "invalid-cursor",
        null,
        10
    );

    // when & then
    assertThatThrownBy(() -> newsArticleRepository.findNewsArticles(request, UUID.randomUUID()))
        .isInstanceOf(InvalidDataAccessApiUsageException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("잘못된 커서 형식");
  }

  @Test
  @DisplayName("QueryDSL - 발행일 커서 기준 다음 페이지 조회에 성공한다.")
  void findNewsArticles_ReturnsNextPage_WhenPublishDateCursorExists() {
    // given

    NewsArticle first = createArticle("first", Instant.now());
    NewsArticle second = createArticle("second", Instant.now().minusSeconds(10));
    NewsArticle third = createArticle("third", Instant.now().minusSeconds(20));

    newsArticleRepository.saveAll(List.of(first, second, third));
    flushAndClear();

    // DB에 저장된 createdAt 기준으로 cursor 생성
    NewsArticle savedSecond = newsArticleRepository.findById(second.getId()).orElseThrow();

    String cursor = savedSecond.getPublishedAt() + "|" + savedSecond.getCreatedAt();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        cursor,
        null,
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result)
        .extracting(NewsArticleCursorRow::sourceUrl)
        .containsExactly("third");
  }

  @Test
  @DisplayName("QueryDSL - 검색어가 요약에 포함된 뉴스기사만 조회한다.")
  void findNewsArticles_ReturnsArticles_WhenKeywordMatchesSummary() {
    // given
    NewsArticle matched = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("summary-match")
        .title("normal title")
        .publishedAt(Instant.now())
        .summary("spring framework article")
        .build();

    NewsArticle notMatched = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("not-match")
        .title("normal title")
        .publishedAt(Instant.now())
        .summary("java article")
        .build();

    newsArticleRepository.saveAll(List.of(matched, notMatched));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        "spring",
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
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).sourceUrl()).isEqualTo("summary-match");
  }

  @Test
  @DisplayName("QueryDSL - 검색어가 공백이면 검색 조건을 적용하지 않는다.")
  void findNewsArticles_ReturnsAllArticles_WhenKeywordIsBlank() {
    // given
    newsArticleRepository.saveAll(List.of(
        createArticle("a", Instant.now()),
        createArticle("b", Instant.now())
    ));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        "   ",
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
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("QueryDSL - 논리 삭제된 뉴스기사는 조회하지 않는다.")
  void findNewsArticles_ExcludesDeletedArticles() {
    // given
    NewsArticle active = createArticle("active", Instant.now());
    NewsArticle deleted = createArticle("deleted", Instant.now().minusSeconds(1));

    deleted.delete(); // 실제 메서드명에 맞게 수정

    newsArticleRepository.saveAll(List.of(active, deleted));
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
        10
    );

    // when
    List<NewsArticleCursorRow> result =
        newsArticleRepository.findNewsArticles(request, UUID.randomUUID());

    // then
    assertThat(result)
        .extracting(NewsArticleCursorRow::sourceUrl)
        .containsExactly("active");
  }

  @Test
  @DisplayName("QueryDSL - 빈 cursor value이면 예외가 발생한다.")
  void findNewsArticles_ThrowsException_WhenCursorValueIsBlank() {
    // given
    newsArticleRepository.save(createArticle("link", Instant.now()));
    flushAndClear();

    String cursor = "|" + Instant.now();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.publishDate,
        NewsArticleDirection.DESC,
        cursor,
        null,
        10
    );

    // when & then
    assertThatThrownBy(() -> newsArticleRepository.findNewsArticles(request, UUID.randomUUID()))
        .isInstanceOf(InvalidDataAccessApiUsageException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
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
  @DisplayName("QueryDSL - count 조회 시 검색어 조건을 적용한다.")
  void countNewsArticles_ReturnsCount_WhenKeywordFilterApplied() {
    // given
    NewsArticle spring = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("spring")
        .title("spring news")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    NewsArticle java = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("java")
        .title("java news")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    newsArticleRepository.saveAll(List.of(spring, java));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        "spring",
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
    assertThat(count).isEqualTo(1L);
  }

  @Test
  @DisplayName("QueryDSL - count 조회 시 출처 조건을 적용한다.")
  void countNewsArticles_ReturnsCount_WhenSourceFilterApplied() {
    // given
    NewsArticle naver = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("naver")
        .title("title")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    NewsArticle yonhap = NewsArticle.builder()
        .source(ArticleSource.YEONHAP)
        .originalLink("yonhap")
        .title("title")
        .publishedAt(Instant.now())
        .summary("summary")
        .build();

    newsArticleRepository.saveAll(List.of(naver, yonhap));
    flushAndClear();

    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        List.of(ArticleSource.NAVER),
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
    assertThat(count).isEqualTo(1L);
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
    NewsArticle article = newsArticleRepository.save(createArticle("link", Instant.now()));
    UUID notExistArticleId = UUID.randomUUID();

    // when
    // 존재하지 않는 ID로 조회수 증가 쿼리 실행
    newsArticleRepository.incrementViewCount(notExistArticleId);

    // 영속성 컨텍스트를 DB와 동기화하고 1차 캐시를 비움
    flushAndClear();

    // then
    assertThat(newsArticleRepository.findById(article.getId()))
        .map(NewsArticle::getViewCount)
        .contains(0L);
  }

  @Test
  @DisplayName("기간 조건으로 뉴스기사 조회에 성공한다(날짜 기준)")
  void findAllByPublishedAtGreaterThanEqualAndPublishedAtLessThan_ReturnsArticles_WhenDateFilterApplied() {
    // given
    LocalDate today = LocalDate.now();
    ZoneId zone = ZoneId.of("Asia/Seoul");

    // 날짜 기준으로 시작/끝 Instant 생성 (KST 기준)
    Instant startOfToday = today.atStartOfDay(zone).toInstant();
    Instant startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();

    // 어제 / 오늘 / 내일 데이터 생성
    NewsArticle yesterday = createArticle(
        "yesterday",
        today.minusDays(1).atStartOfDay(zone).toInstant()
    );

    NewsArticle todayArticle = createArticle(
        "today",
        startOfToday.plusSeconds(3600) // 오늘 1시간 후
    );

    NewsArticle tomorrow = createArticle(
        "tomorrow",
        startOfTomorrow.plusSeconds(3600)
    );

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

  @Test
  @DisplayName("원본 링크 목록으로 여러 개의 뉴스기사를 조회한다.")
  void findAllByOriginalLinkIn_ReturnsMatchedArticles() {
    // given
    NewsArticle a = createArticle("link-a", Instant.now());
    NewsArticle b = createArticle("link-b", Instant.now());
    NewsArticle c = createArticle("link-c", Instant.now());

    newsArticleRepository.saveAll(List.of(a, b, c));
    flushAndClear();

    // when
    List<NewsArticle> result =
        newsArticleRepository.findAllByOriginalLinkIn(List.of("link-a", "link-c"));

    // then
    assertThat(result)
        .extracting(NewsArticle::getOriginalLink)
        .containsExactlyInAnyOrder("link-a", "link-c");
  }

  @Test
  @DisplayName("원본 링크 목록 중 일부만 존재할 경우 존재하는 것만 조회한다.")
  void findAllByOriginalLinkIn_ReturnsPartialMatchedArticles() {
    // given
    NewsArticle a = createArticle("link-a", Instant.now());
    NewsArticle b = createArticle("link-b", Instant.now());

    newsArticleRepository.saveAll(List.of(a, b));
    flushAndClear();

    // when
    List<NewsArticle> result =
        newsArticleRepository.findAllByOriginalLinkIn(List.of("link-a", "not-exist"));

    // then
    assertThat(result)
        .extracting(NewsArticle::getOriginalLink)
        .containsExactly("link-a");
  }

  @Test
  @DisplayName("원본 링크가 모두 존재하지 않으면 빈 리스트를 반환한다.")
  void findAllByOriginalLinkIn_ReturnsEmpty_WhenNoMatch() {
    // given
    newsArticleRepository.save(createArticle("link-a", Instant.now()));
    flushAndClear();

    // when
    List<NewsArticle> result =
        newsArticleRepository.findAllByOriginalLinkIn(List.of("x", "y"));

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("원본 링크 목록이 비어있으면 빈 결과를 반환한다.")
  void findAllByOriginalLinkIn_ReturnsEmpty_WhenInputEmpty() {
    // given
    newsArticleRepository.save(createArticle("link-a", Instant.now()));
    flushAndClear();

    // when
    List<NewsArticle> result =
        newsArticleRepository.findAllByOriginalLinkIn(List.of());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("원본 링크 목록에 중복이 있어도 중복 없이 조회된다.")
  void findAllByOriginalLinkIn_IgnoresDuplicateInput() {
    // given
    NewsArticle a = createArticle("link-a", Instant.now());
    newsArticleRepository.save(a);
    flushAndClear();

    // when
    List<NewsArticle> result =
        newsArticleRepository.findAllByOriginalLinkIn(List.of("link-a", "link-a"));

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOriginalLink()).isEqualTo("link-a");
  }

}
