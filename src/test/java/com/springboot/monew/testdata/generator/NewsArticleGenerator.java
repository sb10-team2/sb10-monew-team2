package com.springboot.monew.testdata.generator;

import static org.instancio.Select.field;

import com.springboot.monew.newsarticle.entity.NewsArticle;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NewsArticleGenerator extends BaseGenerator<NewsArticle> {

  public NewsArticleGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<NewsArticle> run(int size) {
    Model<NewsArticle> model = Instancio.of(NewsArticle.class)
        .supply(field(NewsArticle::getOriginalLink), () -> faker.get().internet().url())
        .supply(field(NewsArticle::getTitle), () -> faker.get().book().title())
        .supply(field(NewsArticle::getSummary), () -> faker.get().lorem().characters(10, 1000))
        .generate(field(NewsArticle::getPublishedAt), this::betweenWeekAndTwoWeeksAgo)
        .generate(field(NewsArticle::getCreatedAt), this::betweenNowAndWeekAgo)
        .set(field(NewsArticle::getViewCount), 0L)
        .set(field(NewsArticle::isDeleted), false)
        .toModel();
    return generate(size, modelGenerator(model));
  }

  @Override
  protected void setValues(PreparedStatement ps, NewsArticle entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, Timestamp.from(entity.getCreatedAt()));
    ps.setObject(3, entity.getSource().name());
    ps.setObject(4, entity.getOriginalLink());
    ps.setObject(5, entity.getTitle());
    ps.setObject(6, Timestamp.from(entity.getPublishedAt()));
    ps.setObject(7, entity.getSummary());
    ps.setObject(8, entity.getViewCount());
    ps.setObject(9, entity.isDeleted());
  }

  @Override
  protected String sql() {
    return insertSql("news_articles", "id", "created_at", "source", "original_link", "title",
        "published_at", "summary", "view_count", "is_deleted");
  }
}
