package com.springboot.monew.exception.user;

import com.springboot.monew.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "이메일 형식이 올바르지 않습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "닉네임 형식이 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호 형식이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
