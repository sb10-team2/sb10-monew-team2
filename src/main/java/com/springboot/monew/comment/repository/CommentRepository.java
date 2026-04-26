package com.springboot.monew.comment.repository;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.repository.qdsl.CommentQDSLRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentQDSLRepository {
  @Query("""
      SELECT c
      FROM Comment c
      JOIN FETCH c.user
      JOIN FETCH c.article
      WHERE c.id = :id
        AND c.isDeleted = false
  """)
  Optional<Comment> findByIdAndIsDeletedFalse(@Param("id") UUID id);

  // increment 시에 영속
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE  c.id = :id")
  void incrementLikeCount(@Param("id") UUID id);

  // bulk update 후 재조회 시 최신 likeCount를 반영할 수 있도록 영속성 컨텍스트를 초기화한다.
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
              UPDATE Comment c
              SET c.likeCount = CASE
              WHEN c.likeCount > 0 THEN c.likeCount - 1
              ELSE 0
              END
              WHERE c.id = :id
      """)
  void decrementLikeCount(@Param("id") UUID id);

  long countByArticleIdAndIsDeletedFalse(UUID articleId);
}
