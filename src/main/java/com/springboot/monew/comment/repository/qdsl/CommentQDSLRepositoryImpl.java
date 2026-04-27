package com.springboot.monew.comment.repository.qdsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentDirection;
import com.springboot.monew.comment.entity.CommentOrderBy;
import com.springboot.monew.comment.entity.QComment;
import com.springboot.monew.users.entity.QUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQDSLRepositoryImpl implements CommentQDSLRepository{

  private final JPAQueryFactory queryFactory;
  private final QComment qComment = QComment.comment;
  private final QUser qUser = QUser.user;

  @Override
  public List<Comment> findComments(UUID articleId, CommentOrderBy orderBy,
      CommentDirection direction, String cursor, Instant after, int limit) {

    return queryFactory
        .selectFrom(qComment)
        .join(qComment.user, qUser).fetchJoin()
        .where(
            qComment.article.id.eq(articleId),
            qComment.isDeleted.eq(false),
            cursorCondition(orderBy, direction, cursor, after)
        )
        .orderBy(orderByCondition(orderBy, direction))
        .limit(limit)
        .fetch();
  }

  private BooleanExpression cursorCondition(CommentOrderBy orderBy,
      CommentDirection direction, String cursor, Instant after) {

    if (orderBy == CommentOrderBy.likeCount) {
      if (cursor == null) return null;

      // "likeCount|createdAt" 파싱, after는 fallback
      String[] parts = cursor.split("\\|", 2);
      long likeCountCursor = Long.parseLong(parts[0]);
      Instant afterCursor = parts.length > 1 ? Instant.parse(parts[1]) : after;

      if (direction == CommentDirection.ASC) {
        BooleanExpression primary = qComment.likeCount.gt(likeCountCursor);
        if (afterCursor == null) return primary;
        return primary.or(qComment.likeCount.eq(likeCountCursor)
            .and(qComment.createdAt.lt(afterCursor)));
      } else {
        BooleanExpression primary = qComment.likeCount.lt(likeCountCursor);
        if (afterCursor == null) return primary;
        return primary.or(qComment.likeCount.eq(likeCountCursor)
            .and(qComment.createdAt.gt(afterCursor)));
      }
    }

    // createdAt 정렬
    Instant createdAtCursor = after != null ? after : (cursor != null ? Instant.parse(cursor) : null);
    if (createdAtCursor == null) return null;

    return direction == CommentDirection.DESC
        ? qComment.createdAt.lt(createdAtCursor)
        : qComment.createdAt.gt(createdAtCursor);
  }


  // 정렬 조건
  private OrderSpecifier<?>[] orderByCondition(CommentOrderBy orderBy, CommentDirection direction) {
    if (orderBy == CommentOrderBy.likeCount) {
      return direction == CommentDirection.DESC
          ? new OrderSpecifier<?>[] {qComment.likeCount.desc(), qComment.createdAt.asc()}
          : new OrderSpecifier<?>[] {qComment.likeCount.asc(), qComment.createdAt.desc()};
    }
    return direction == CommentDirection.DESC
        ? new OrderSpecifier<?>[] {qComment.createdAt.desc()}
        : new OrderSpecifier<?>[] {qComment.createdAt.asc()};
  }

}
