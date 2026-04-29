package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.user.entity.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Setter;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ArticleViewGenerator extends BaseGenerator<ArticleView> {

  @Setter
  private int articlePerUser = 10;

  public ArticleViewGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<ArticleView> run(List<User> users, List<NewsArticle> articles) {
    AtomicInteger offset = new AtomicInteger(0);
    return generate(users.size(),
        relationMappingGenerator(users, offset, user -> createArticlesFor(user, articles)));
  }

  private Stream<ArticleView> createArticlesFor(User user, List<NewsArticle> articles) {
    return uniqueRandomNumbers(articles.size(), articlePerUser).stream()
        .map(idx -> Instancio.of(ArticleView.class)
            .generate(field(ArticleView::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
            .set(field(ArticleView::getUser), user)
            .set(field(ArticleView::getNewsArticle), articles.get(idx))
            .create());
  }

  @Override
  protected void setValues(PreparedStatement ps, ArticleView entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getNewsArticle().getId());
    ps.setObject(3, entity.getUser().getId());
    ps.setObject(4, Timestamp.from(entity.getCreatedAt()));
  }

  @Override
  protected String sql() {
    return insertSql("article_views", "id", "news_article_id", "user_id", "created_at");
  }

  @Override
  protected int batchSize() {
    if (super.batchSize() < articlePerUser) {
      throw new IllegalArgumentException(
          "articlePerUser=%s 는 1000을 넘길 수 없다".formatted(articlePerUser));
    }
    return Math.max(1, super.batchSize() / articlePerUser);
  }
}
