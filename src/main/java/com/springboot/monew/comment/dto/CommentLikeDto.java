package com.springboot.monew.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeDto(
    UUID id,
    // 좋아요한 사용자
    UUID likeBy,
    // 좋아요한 날짜
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    // 댓글 작성자 Id
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Integer commentLikeCount,
    Instant commentCreatedAt) {}
