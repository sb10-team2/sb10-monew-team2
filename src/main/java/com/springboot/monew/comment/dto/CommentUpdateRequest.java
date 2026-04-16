package com.springboot.monew.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 200, message = "수정할 댓글은 200자 이내로 입력해주세요.")
        String content
) {
}
