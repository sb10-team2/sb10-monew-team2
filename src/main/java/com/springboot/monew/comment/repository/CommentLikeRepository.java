package com.springboot.monew.comment.repository;

import com.springboot.monew.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    // TODO: User 구현 시 주석 해제
    // boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);
}
