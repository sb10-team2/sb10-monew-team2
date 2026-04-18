package com.springboot.monew.interest.exception;

import com.springboot.monew.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestErrorCode implements ErrorCode {
    // 관심사 이름이 이미 존재하는 예외와 80% 이상 유사한 관심사 이름이 존재하는 예외의 메시지를 동일하게 처리
    INTEREST_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "IN01", "유사한 이름의 관심사가 이미 존재합니다."),
    INTEREST_NAME_SIMILARITY_CONFLICT(HttpStatus.CONFLICT, "IN02", "유사한 이름의 관심사가 이미 존재합니다."),
    DUPLICATE_KEYWORDS(HttpStatus.BAD_REQUEST, "IN03", "키워드는 중복될 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
