package com.springboot.monew.comment.repository;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    Optional<CommentLike> findCommentLikeByCommentAndUser(Comment comment, User user);
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);


    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    List<UUID> findCommentIdsByUserIdAndCommentIdIn(@Param("userId") UUID userId, @Param("commentIds") List<UUID> commentIds);
}
