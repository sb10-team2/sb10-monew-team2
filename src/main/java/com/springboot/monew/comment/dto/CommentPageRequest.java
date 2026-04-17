package com.springboot.monew.comment.dto;

import com.springboot.monew.comment.entity.CommentDirection;
import com.springboot.monew.comment.entity.CommentOrderBy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Instant;
import java.util.UUID;

// 댓글 목록 조회 Request parameter
// 요청이 너무 많아 따로 Dto로 분리하였음
public record CommentPageRequest(
        UUID articleId,
        @NotNull
        CommentOrderBy orderBy,
        @NotNull
        CommentDirection direction,
        String cursor,
        Instant after,
        @Min(1)
        @Max(100)
        @DefaultValue("50")
        int limit
) {
}
