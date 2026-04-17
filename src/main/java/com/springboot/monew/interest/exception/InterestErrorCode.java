package com.springboot.monew.interest.exception;

import com.springboot.monew.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestErrorCode implements ErrorCode {
    INTEREST_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "IN01", "유사한 이름의 관심사가 이미 존재합니다."),
    DUPLICATE_KEYWORDS(HttpStatus.BAD_REQUEST, "IN02", "키워드는 중복될 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
