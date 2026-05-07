package com.springboot.monew.interest.repository.qdsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.interest.dto.request.InterestPageRequest;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestDirection;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.InterestOrderBy;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.newsarticle.entity.ArticleInterest;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import java.time.Instant;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.util.ReflectionTestUtils;

class InterestQDSLRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private InterestRepository repository;
  @Autowired
  private InterestRepository interestRepository;

  @BeforeEach
  void setUp() {
    queryInspector.clear();
  }

  @Test
  @DisplayName("관심사 조회 시 관심사와 연관된 기사 갯수와 함께 조회할 수 있다\n"
      + "관심사와 기사 10개가 연관 되어 있다")
  void findByIdWithArticleCount() {
    // given
    int articleCount = 10;
    Interest expected = testEntityManager.generateInterest();
    expected.setArticleCount(articleCount);
    List<NewsArticle> articles = testEntityManager.generateNewsArticles(articleCount);
    articles.stream()
        .map(article -> new ArticleInterest(article, expected))
        .forEach(this.em::persist);
    em.flush();
    clear();

    // when
    Interest actual = repository.findByIdWithArticleCount(expected.getId()).orElseThrow();
    printQueries();
    ensureQueryCount(1);

    // then
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .withEqualsForType(this::compareInstant, Instant.class)
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("관심사를 이름 오름차순으로 조회할 수 있다")
  void findInterests_ReturnsInterestsSortedByNameAsc_WhenOrderByNameAsc() {
    // given
    saveInterest("Gamma");
    saveInterest("Alpha");
    saveInterest("Beta");
    flushAndClear();

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("Alpha", "Beta", "Gamma");
  }

  @Test
  @DisplayName("관심사를 이름 내림차순으로 조회할 수 있다")
  void findInterests_ReturnsInterestsSortedByNameDesc_WhenOrderByNameDesc() {
    // given
    saveInterest("Gamma");
    saveInterest("Alpha");
    saveInterest("Beta");
    flushAndClear();

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.DESC,
        null,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("Gamma", "Beta", "Alpha");
  }

  @Test
  @DisplayName("관심사를 구독자 수 오름차순으로 조회할 수 있다")
  void findInterests_ReturnsInterestsSortedBySubscriberCountAsc_WhenOrderBySubscriberCountAsc() {
    // given
    Interest low = saveInterest("low");
    setSubscriberCount(low, 1);
    Interest middle = saveInterest("middle");
    setSubscriberCount(middle, 3);
    Interest high = saveInterest("high");
    setSubscriberCount(high, 5);
    flushAndClear();

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.ASC,
        null,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("low", "middle", "high");
  }

  @Test
  @DisplayName("관심사를 구독자 수 내림차순으로 조회할 수 있다")
  void findInterests_ReturnsInterestsSortedBySubscriberCountDesc_WhenOrderBySubscriberCountDesc() {
    // given
    Interest low = saveInterest("low-desc");
    setSubscriberCount(low, 1);
    Interest middle = saveInterest("middle-desc");
    setSubscriberCount(middle, 3);
    Interest high = saveInterest("high-desc");
    setSubscriberCount(high, 5);
    flushAndClear();

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.DESC,
        null,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName)
        .containsExactly("high-desc", "middle-desc", "low-desc");
  }

  @Test
  @DisplayName("관심사 이름으로 검색할 수 있다")
  void findInterests_ReturnsMatchedInterests_WhenKeywordMatchesInterestName() {
    // given
    saveInterest("finance");
    saveInterest("technology");
    saveInterest("sports");
    flushAndClear();

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        "fin",
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("finance");
  }

  @Test
  @DisplayName("키워드 이름으로 검색할 수 있다")
  void findInterests_ReturnsMatchedInterests_WhenKeywordMatchesKeywordName() {
    // given
    Interest finance = saveInterest("finance-keyword");
    linkKeyword(finance, "economy");
    Interest tech = saveInterest("tech-keyword");
    linkKeyword(tech, "ai");
    flushAndClear();

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        "eco",
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("finance-keyword");
  }

  @Test
  @DisplayName("검색어가 비어 있으면 전체 관심사를 조회한다")
  void findInterests_ReturnsAllInterests_WhenKeywordIsBlank() {
    // given
    saveInterest("alpha");
    saveInterest("beta");
    saveInterest("gamma");
    flushAndClear();

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        "   ",
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("alpha", "beta", "gamma");
  }

  @Test
  @DisplayName("커서 이후의 관심사 목록을 조회할 수 있다")
  void findInterests_ReturnsNextPage_WhenCursorExists() {
    // given
    Interest alpha = saveInterest("alpha");
    saveInterest("beta");
    saveInterest("gamma");
    flushAndClear();

    // when
    Instant alphaCreatedAt = interestRepository.findById(alpha.getId()).orElseThrow()
        .getCreatedAt();
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.ASC,
        "alpha",
        alphaCreatedAt,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("beta", "gamma");
  }

  @Test
  @DisplayName("이름 내림차순 커서 이후의 관심사 목록을 조회할 수 있다")
  void findInterests_ReturnsNextPage_WhenOrderByNameDescAndCursorExists() {
    // given
    saveInterest("Alpha");
    Interest beta = saveInterest("Beta");
    saveInterest("Gamma");
    flushAndClear();

    Instant betaCreatedAt = interestRepository.findById(beta.getId()).orElseThrow().getCreatedAt();

    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.DESC,
        "Beta",
        betaCreatedAt,
        10
    );

    // when
    List<Interest> result = interestRepository.findInterests(request);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("Alpha");
  }

  @Test
  @DisplayName("이름 커서만 있고 after가 없으면 이름 기준으로 다음 페이지를 조회한다")
  void findInterests_ReturnsNextPage_WhenOrderByNameAndAfterIsNull() {
    // given
    saveInterest("Alpha");
    saveInterest("Beta");
    saveInterest("Gamma");
    flushAndClear();

    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.ASC,
        "Alpha",
        null,
        10
    );

    // when
    List<Interest> result = interestRepository.findInterests(request);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("Beta", "Gamma");
  }

  @Test
  @DisplayName("구독자 수 커서 이후의 관심사 목록을 조회할 수 있다")
  void findInterests_ReturnsNextPage_WhenSubscriberCountCursorExists() {
    // given
    Interest high = saveInterest("high");
    setSubscriberCount(high, 5);
    Interest olderSameCount = saveInterest("older");
    setSubscriberCount(olderSameCount, 3);
    pause();
    Interest newerSameCount = saveInterest("newer");
    setSubscriberCount(newerSameCount, 3);
    Interest low = saveInterest("low");
    setSubscriberCount(low, 1);
    flushAndClear();

    // when
    Instant newerCreatedAt = interestRepository.findById(newerSameCount.getId()).orElseThrow()
        .getCreatedAt();
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.DESC,
        "3",
        newerCreatedAt,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName).containsExactly("older", "low");
  }

  @Test
  @DisplayName("생성 시각이 포함된 구독자 수 커서 이후의 관심사 목록을 조회할 수 있다")
  void findInterests_ReturnsNextPage_WhenSubscriberCountCursorContainsCreatedAt() {
    // given
    Interest high = saveInterest("high-cursor");
    setSubscriberCount(high, 5);
    Interest olderSameCount = saveInterest("older-cursor");
    setSubscriberCount(olderSameCount, 3);
    pause();
    Interest newerSameCount = saveInterest("newer-cursor");
    setSubscriberCount(newerSameCount, 3);
    Interest low = saveInterest("low-cursor");
    setSubscriberCount(low, 1);
    flushAndClear();

    Instant newerCreatedAt = interestRepository.findById(newerSameCount.getId()).orElseThrow()
        .getCreatedAt();
    String cursor = "3|" + newerCreatedAt;

    // when
    List<Interest> result = interestRepository.findInterests(new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.DESC,
        cursor,
        null,
        10
    ));

    // then
    assertThat(result).extracting(Interest::getName)
        .containsExactly("older-cursor", "low-cursor");
  }

  @Test
  @DisplayName("구독자 수 오름차순 커서 이후의 관심사 목록을 조회할 수 있다")
  void findInterests_ReturnsNextPage_WhenOrderBySubscriberCountAscAndCursorExists() {
    // given
    Interest low = saveInterest("low-asc-cursor");
    setSubscriberCount(low, 1);

    Interest olderSameCount = saveInterest("older-asc-cursor");
    setSubscriberCount(olderSameCount, 3);

    pause();

    Interest newerSameCount = saveInterest("newer-asc-cursor");
    setSubscriberCount(newerSameCount, 3);

    Interest high = saveInterest("high-asc-cursor");
    setSubscriberCount(high, 5);

    flushAndClear();

    Instant olderCreatedAt = interestRepository.findById(olderSameCount.getId())
        .orElseThrow()
        .getCreatedAt();

    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.ASC,
        "3",
        olderCreatedAt,
        10
    );

    // when
    List<Interest> result = interestRepository.findInterests(request);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("newer-asc-cursor", "high-asc-cursor");
  }

  @Test
  @DisplayName("구독자 수 커서만 있고 after가 없으면 구독자 수 기준으로 다음 페이지를 조회한다")
  void findInterests_ReturnsNextPage_WhenOrderBySubscriberCountAndAfterIsNull() {
    // given
    Interest low = saveInterest("low-asc-no-after");
    setSubscriberCount(low, 1);

    Interest middle = saveInterest("middle-asc-no-after");
    setSubscriberCount(middle, 3);

    Interest high = saveInterest("high-asc-no-after");
    setSubscriberCount(high, 5);

    flushAndClear();

    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.ASC,
        "3",
        null,
        10
    );

    // when
    List<Interest> result = interestRepository.findInterests(request);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("high-asc-no-after");
  }

  @Test
  @DisplayName("커서가 공백이면 첫 페이지를 조회한다")
  void findInterests_ReturnsFirstPage_WhenCursorIsBlank() {
    // given
    saveInterest("Alpha-blank-cursor");
    saveInterest("Beta-blank-cursor");
    saveInterest("Gamma-blank-cursor");
    flushAndClear();

    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.ASC,
        "   ",
        null,
        10
    );

    // when
    List<Interest> result = interestRepository.findInterests(request);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("Alpha-blank-cursor", "Beta-blank-cursor", "Gamma-blank-cursor");
  }

  @Test
  @DisplayName("잘못된 구독자 수 커서를 전달하면 예외가 발생한다")
  void findInterests_ThrowsException_WhenSubscriberCountCursorIsInvalid() {
    // given
    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.ASC,
        "not-a-number",
        null,
        10
    );

    // when
    ThrowingCallable action = () -> interestRepository.findInterests(request);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InvalidDataAccessApiUsageException.class)
        .hasMessageContaining("Invalid subscriberCount cursor");
  }

  @Test
  @DisplayName("구독자 수 커서의 생성 시각 형식이 잘못되면 예외가 발생한다")
  void findInterests_ThrowsException_WhenSubscriberCountCursorCreatedAtIsInvalid() {
    // given
    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.DESC,
        "3|invalid-created-at",
        null,
        10
    );

    // when
    ThrowingCallable action = () -> interestRepository.findInterests(request);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InvalidDataAccessApiUsageException.class)
        .hasMessageContaining("Invalid subscriberCount cursor");
  }

  @Test
  @DisplayName("검색 조건에 맞는 관심사 수를 조회할 수 있다")
  void countInterests_ReturnsMatchedCount_WhenKeywordExists() {
    // given
    Interest first = saveInterest("eco focus");
    linkKeyword(first, "eco");
    linkKeyword(first, "economy");
    Interest second = saveInterest("finance");
    linkKeyword(second, "eco system");
    Interest third = saveInterest("sports");
    linkKeyword(third, "baseball");
    flushAndClear();

    // when
    long result = interestRepository.countInterests("eco");

    // then
    assertThat(result).isEqualTo(2L);
  }

  @Test
  @DisplayName("검색어가 비어 있으면 전체 관심사 수를 조회한다")
  void countInterests_ReturnsAllCount_WhenKeywordIsBlank() {
    // given
    saveInterest("alpha-count");
    saveInterest("beta-count");
    saveInterest("gamma-count");
    flushAndClear();

    // when
    long result = interestRepository.countInterests("   ");

    // then
    assertThat(result).isEqualTo(3L);
  }

  private Interest saveInterest(String name) {
    Interest interest = new Interest(name);
    em.persist(interest);
    em.flush();
    return interest;
  }

  private Keyword saveKeyword(String name) {
    Keyword keyword = new Keyword(name);
    em.persist(keyword);
    em.flush();
    return keyword;
  }

  private void linkKeyword(Interest interest, String keywordName) {
    em.persist(new InterestKeyword(interest, saveKeyword(keywordName)));
    em.flush();
  }

  private void setSubscriberCount(Interest interest, long subscriberCount) {
    ReflectionTestUtils.setField(interest, "subscriberCount", subscriberCount);
    em.flush();
  }

  private void pause() {
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("sleep interrupted", e);
    }
  }
}
