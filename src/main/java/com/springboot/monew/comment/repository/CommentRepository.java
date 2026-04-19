package com.springboot.monew.comment.repository;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentDirection;
import com.springboot.monew.comment.entity.CommentOrderBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.id = :id AND c.isDeleted = false")
    Optional<Comment> findByIdAndIsDeletedFalse(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE  c.id = :id")
    void incrementLikeCount(@Param("id") UUID id);

    @Modifying
    @Query("""
            UPDATE Comment c
            SET c.likeCount = CASE
            WHEN c.likeCount > 0 THEN c.likeCount - 1
            ELSE 0
            END
            WHERE c.id = :id
    """)
    void decrementLikeCount(@Param("id") UUID id);

    // Todo: users 쪽 Join 테이블 필요해보임 -> users 쪽 완전 구현 완료 시에 다시 check, 인덱스 고려
    @Query(value = """
      SELECT * FROM comments
      WHERE article_id = :articleId
      AND is_deleted = false
      AND (:cursor IS NULL OR
          CASE WHEN :orderBy = 'LIKE_COUNT' THEN
              (like_count < :cursor OR (like_count = :cursor AND created_at < :after))
          ELSE
              created_at < :after
          END
      )
      ORDER BY
          CASE WHEN :orderBy = 'LIKE_COUNT' AND :direction = 'DESC' THEN like_count END DESC,
          CASE WHEN :orderBy = 'LIKE_COUNT' AND :direction = 'ASC' THEN like_count END ASC,
          created_at DESC
      LIMIT :limit
      """, nativeQuery = true)
    List<Comment> findComments(
            @Param("articleId") UUID articleId,
            @Param("orderBy") CommentOrderBy orderBy,
            @Param("direction") CommentDirection direction,
            @Param("cursor") String cursor,
            @Param("after") Instant after,
            @Param("limit") int limit
    );
}
