package com.springboot.monew.comment.dto;

import java.time.Instant;
import java.util.UUID;

// 댓글 응답 dto
public record CommentDto(
        UUID id,
        UUID articleId,
        UUID userId,
        String userNickname,
        String content,
        Integer likeCount,
        Boolean likeByMe,
        Instant createdAt
) {
}
