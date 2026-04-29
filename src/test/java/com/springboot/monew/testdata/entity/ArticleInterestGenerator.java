package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ArticleInterestGenerator extends BaseGenerator<ArticleInterest> {

  public ArticleInterestGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<ArticleInterest> run(List<Interest> interests, List<NewsArticle> articles) {
    AtomicInteger offset = new AtomicInteger(0);
    return generate(interests.size(), relationMappingGenerator(interests, offset,
        interest -> createArticlesFor(interest, articles)));
  }

  private Stream<ArticleInterest> createArticlesFor(Interest interest, List<NewsArticle> articles) {
    return uniqueRandomNumbers(articles,
        () -> ThreadLocalRandom.current().nextInt(1, 101))
        .stream()
        .map(idx -> Instancio.of(ArticleInterest.class)
            .generate(field(ArticleInterest::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
            .set(field(ArticleInterest::getInterest), interest)
            .set(field(ArticleInterest::getNewsArticle), articles.get(idx))
            .create());
  }

  @Override
  protected void setValues(PreparedStatement ps, ArticleInterest entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getNewsArticle().getId());
    ps.setObject(3, entity.getInterest().getId());
    ps.setObject(4, Timestamp.from(entity.getCreatedAt()));
  }

  @Override
  protected String sql() {
    return insertSql("article_interests", "id", "news_article_id", "interest_id", "created_at");
  }

  @Override
  protected int batchSize() {
    return super.batchSize() / 5;
  }
}
