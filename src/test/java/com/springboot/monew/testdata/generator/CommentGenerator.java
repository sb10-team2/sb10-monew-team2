package com.springboot.monew.testdata.generator;

import static org.instancio.Select.field;

import com.springboot.monew.comment.entity.Comment;
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
public class CommentGenerator extends BaseGenerator<Comment> {

  @Setter
  private int commentPerUser = 50;

  public CommentGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<Comment> run(List<User> users, List<NewsArticle> articles) {
    AtomicInteger offset = new AtomicInteger(0);
    return generate(users.size(), relationMappingGenerator(users, offset,
        user -> createCommentsFor(user, articles)));
  }

  private Stream<Comment> createCommentsFor(User user, List<NewsArticle> articles) {
    return uniqueRandomNumbers(articles.size(), commentPerUser).stream()
        .map(idx -> Instancio.of(Comment.class)
            .generate(field(Comment::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
            .supply(field(Comment::getContent), () -> faker.get().lorem().characters(5, 200))
            .set(field(Comment::getUser), user)
            .set(field(Comment::getArticle), articles.get(idx))
            .set(field(Comment::getLikeCount), 0L)
            .set(field(Comment::isDeleted), false)
            .ignore(field(Comment::getUpdatedAt))
            .create());
  }

  @Override
  protected void setValues(PreparedStatement ps, Comment entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getUser().getId());
    ps.setObject(3, entity.getArticle().getId());
    ps.setObject(4, entity.getContent());
    ps.setObject(5, entity.getLikeCount());
    ps.setObject(6, entity.isDeleted());
    ps.setObject(7, Timestamp.from(entity.getCreatedAt()));
    ps.setObject(8, entity.getUpdatedAt());
  }

  @Override
  protected String sql() {
    return insertSql("comments", "id", "user_id", "article_id", "content", "like_count",
        "is_deleted", "created_at", "updated_at");
  }

  @Override
  protected int batchSize() {
    if (super.batchSize() < commentPerUser) {
      throw new IllegalArgumentException(
          "commentPerUser=%s 는 1000을 넘길 수 없다".formatted(commentPerUser));
    }
    return Math.max(1, super.batchSize() / commentPerUser);
  }
}
