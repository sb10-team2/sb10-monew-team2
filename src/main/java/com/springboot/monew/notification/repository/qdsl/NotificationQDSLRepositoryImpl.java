package com.springboot.monew.notification.repository.qdsl;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.comment.entity.QCommentLike;
import com.springboot.monew.interest.entity.QInterest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.QNotification;
import com.springboot.monew.user.entity.QUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQDSLRepositoryImpl implements NotificationQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private final QNotification qNotification = QNotification.notification;
  private final QInterest qInterest = QInterest.interest;
  private final QCommentLike qCommentLike = QCommentLike.commentLike;
  private final QUser qUser = QUser.user;

  @Override
  public Slice<Notification> findByCursor(UUID cursor, Instant after, UUID userId,
      Pageable pageable) {
    int pageSize = pageable.getPageSize();
    List<Notification> content = queryFactory.selectFrom(qNotification)
        .join(qNotification.user, qUser)
        .leftJoin(qNotification.interest, qInterest).fetchJoin()
        .leftJoin(qNotification.commentLike, qCommentLike).fetchJoin()
        .where(
            qNotification.user.id.eq(userId),
            qNotification.confirmed.isFalse(),
            cursorCondition(cursor, after)
        )
        .orderBy(qNotification.createdAt.desc(), qNotification.id.asc())
        .limit(pageSize + 1)
        .fetch();
    return toSlice(content, pageSize);
  }

  private Predicate cursorCondition(UUID cursor, Instant after) {
    if (cursor == null || after == null) {
      return null;
    }
    return qNotification.createdAt.lt(after)
        .or(qNotification.createdAt.eq(after).and(qNotification.id.gt(cursor)));
  }

  private Slice<Notification> toSlice(List<Notification> content, int pageSize) {
    boolean hasNext = false;
    if (content.size() > pageSize) {
      content.remove(pageSize);
      hasNext = true;
    }
    return new SliceImpl<>(content, Pageable.unpaged(), hasNext);
  }
}
