package com.springboot.monew.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CommentActivityDto(
    @Schema(description = "댓글 ID", format = "uuid")
    UUID id,

    @Schema(description = "기사 ID", format = "uuid")
    UUID articleId,

    @Schema(description = "기사 제목")
    String articleTitle,

    @Schema(description = "작성자 ID", format = "uuid")
    UUID userId,

    @Schema(description = "작성자 닉네임")
    String userNickname,

    @Schema(description = "내용")
    String content,

    @Schema(description = "좋아요 수")
    Long likeCount,

    @Schema(description = "작성된 날짜", format = "date-time")
    Instant createdAt
) {

}
