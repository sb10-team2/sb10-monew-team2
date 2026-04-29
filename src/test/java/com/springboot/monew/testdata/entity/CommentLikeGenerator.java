package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
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
public class CommentLikeGenerator extends BaseGenerator<CommentLike> {

  @Setter
  private int commentLikePerUser = 50;

  public CommentLikeGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<CommentLike> run(List<User> users, List<Comment> comments) {
    AtomicInteger offset = new AtomicInteger(0);
    return generate(users.size(), relationMappingGenerator(users, offset,
        interest -> createCommentLikesFor(interest, comments)));
  }

  private Stream<CommentLike> createCommentLikesFor(User user, List<Comment> comments) {
    return uniqueRandomNumbers(comments.size(), commentLikePerUser).stream()
        .map(idx -> Instancio.of(CommentLike.class)
            .generate(field(CommentLike::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
            .set(field(CommentLike::getUser), user)
            .set(field(CommentLike::getComment), comments.get(idx))
            .create());
  }

  @Override
  protected void setValues(PreparedStatement ps, CommentLike entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, Timestamp.from(entity.getCreatedAt()));
    ps.setObject(3, entity.getComment().getId());
    ps.setObject(4, entity.getUser().getId());
  }

  @Override
  protected String sql() {
    return insertSql("comment_likes", "id", "created_at", "comment_id", "user_id");
  }

  @Override
  protected int batchSize() {
    if (super.batchSize() < commentLikePerUser) {
      throw new IllegalArgumentException(
          "commentLikePerUser=%s 는 1000을 넘길 수 없다".formatted(commentLikePerUser));
    }
    return Math.max(1, super.batchSize() / commentLikePerUser);
  }
}
