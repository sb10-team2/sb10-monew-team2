package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.user.entity.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Setter;
import org.instancio.Instancio;
import org.instancio.TargetSelector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationGenerator extends BaseGenerator<Notification> {

  @Setter
  private int notificationPerUser = 50;

  public NotificationGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<Notification> run(List<User> users, List<CommentLike> commentLikes,
      List<Interest> interests) {
    List<CommentLike> shuffledCls = shuffledCopy(commentLikes);
    List<Interest> shuffledInts = shuffledCopy(interests);

    AtomicInteger offset = new AtomicInteger(0);
    AtomicInteger clPtr = new AtomicInteger(0);
    AtomicInteger intPtr = new AtomicInteger(0);
    int count = notificationPerUser / 2;

    return generate(users.size(), relationMappingGenerator(users, offset,
        user -> createMixedNotifications(user, shuffledCls, clPtr, shuffledInts, intPtr, count)
    ));
  }

  private Stream<Notification> createMixedNotifications(
      User user, List<CommentLike> cls, AtomicInteger clPtr,
      List<Interest> ints, AtomicInteger intPtr, int count) {

    return Stream.concat(
        createNotifications(user, popSlice(cls, clPtr, count), ResourceType.COMMENT,
            field(Notification::getCommentLike), field(Notification::getInterest)),
        createNotifications(user, popSlice(ints, intPtr, count), ResourceType.INTEREST,
            field(Notification::getInterest), field(Notification::getCommentLike))
    );
  }

  private <T> List<T> shuffledCopy(List<T> source) {
    List<T> copy = new ArrayList<>(source);
    Collections.shuffle(copy);
    return copy;
  }

  private <T> List<T> popSlice(List<T> source, AtomicInteger pointer, int size) {
    int start = pointer.getAndAdd(size);
    return start + size <= source.size()
        ? source.subList(start, start + size)
        : Collections.emptyList();
  }

  private <T> Stream<Notification> createNotifications(
      User user,
      List<T> targets,
      ResourceType type,
      TargetSelector setField,
      TargetSelector ignoreField) {
    return uniqueRandomNumbers(targets.size(), notificationPerUser / 2).stream()
        .map(idx -> Instancio.of(Notification.class)
            .generate(field(Notification::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
            .set(field(Notification::getUser), user)
            .set(field(Notification::getConfirmed), false)
            .supply(field(Notification::getContent), () -> faker.get().lorem().characters(3, 100))
            .ignore(field(Notification::getUpdatedAt))
            .set(field(Notification::getResourceType), type)
            .set(setField, targets.get(idx))
            .ignore(ignoreField)
            .create());
  }

  @Override
  protected void setValues(PreparedStatement ps, Notification entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, Timestamp.from(entity.getCreatedAt()));
    ps.setObject(3, entity.getUpdatedAt());
    ps.setObject(4, entity.getConfirmed());
    ps.setObject(5, entity.getContent());
    ps.setObject(6, entity.getResourceType().name());
    ps.setObject(7, entity.getUser().getId());
    ps.setObject(8, entity.getInterest() != null ? entity.getInterest().getId() : null);
    ps.setObject(9, entity.getCommentLike() != null ? entity.getCommentLike().getId() : null);
  }

  @Override
  protected String sql() {
    return insertSql("notifications", "id", "created_at", "updated_at",
        "confirmed", "content", "resource_type", "user_id", "interest_id", "comment_likes_id");
  }

  @Override
  protected int batchSize() {
    if (super.batchSize() < notificationPerUser) {
      throw new IllegalArgumentException(
          "notificationPerUser=%s 는 1000을 넘길 수 없다".formatted(notificationPerUser));
    }
    return Math.max(1, super.batchSize() / notificationPerUser);
  }
}
