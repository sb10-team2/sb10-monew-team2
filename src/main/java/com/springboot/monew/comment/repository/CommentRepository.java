package com.springboot.monew.comment.repository;

import com.springboot.monew.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Optional<Comment> findByIdAndIsDeletedFalse(UUID id);

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
}
