package com.springboot.monew.users.exception;

import com.springboot.monew.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "이메일 형식이 올바르지 않습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "닉네임 형식이 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호 형식이 올바르지 않습니다."),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
