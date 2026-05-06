package com.springboot.monew.interest.repository.qdsl;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.newsarticle.entity.ArticleInterest;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import java.time.Instant;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InterestQDSLRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private InterestRepository repository;

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
}
