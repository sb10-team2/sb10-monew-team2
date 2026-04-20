package com.springboot.monew.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

// 댓글 등록 요청 Dto
public record CommentRegisterRequest(
    @NotNull(message = "articleId는 필수입니다.") UUID articleId,
    @NotNull(message = "userId는 필수입니다.") UUID userId,
    @NotBlank(message = "내용을 입력해주세요.") @Size(max = 200, message = "댓글은 200자 이내로 입력해주세요.")
        String content) {}
