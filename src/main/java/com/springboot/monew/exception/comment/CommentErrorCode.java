package com.springboot.monew.exception.comment;

import com.springboot.monew.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM01", "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_OWNED_BY_USER(HttpStatus.FORBIDDEN, "CM02", "본인의 댓글이 아닙니다."),
    COMMENT_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CM03", "이미 좋아요를 눌렀습니다."),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "CM04", "좋아요를 누르지 않았습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public String getCode() {
        return code;
    }
}
