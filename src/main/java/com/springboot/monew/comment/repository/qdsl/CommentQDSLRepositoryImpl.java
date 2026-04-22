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
    if (cursor == null || after == null) {return null;}

    if(orderBy == CommentOrderBy.likeCount) {
      long likeCountCursor = Long.parseLong(cursor);
      if (direction == CommentDirection.ASC) {
        return qComment.likeCount.gt(likeCountCursor)
            .or(qComment.likeCount.eq(likeCountCursor)
                .and(qComment.createdAt.lt(after)));
      } else {
        return qComment.likeCount.lt(likeCountCursor)
            .or(qComment.likeCount.eq(likeCountCursor)
                .and(qComment.createdAt.gt(after)));
      }
    }

    return direction == CommentDirection.DESC
        ? qComment.createdAt.lt(after)
        : qComment.createdAt.gt(after);
  }

  // 정렬 조건
  private OrderSpecifier<?> orderByCondition(CommentOrderBy orderBy, CommentDirection direction) {
    if (orderBy == CommentOrderBy.likeCount) {
      return direction == CommentDirection.DESC
          ? qComment.likeCount.desc()
          : qComment.likeCount.asc();
    }
    return direction == CommentDirection.DESC
        ? qComment.createdAt.desc()
        : qComment.createdAt.asc();
  }

}
