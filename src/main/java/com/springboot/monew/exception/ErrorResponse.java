package com.springboot.monew.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private final Instant timestamp;
    private final String code;
    private final String message;
    private final Map<String, Object> details;
    private final String exceptionType;
    private final int status;

    // 커스텀 예외 처리용
    public static ErrorResponse from(MonewException ex) {
        return new ErrorResponse(
                Instant.now(),
                ex.getErrorCode().name(),
                ex.getMessage(),
                ex.getDetails(),
                ex.getClass().getSimpleName(),
                ex.getErrorCode().getHttpStatus().value()
        );
    }

    // @Valid 검증 실패 처리용 (필드별 에러 details 포함)
    public static ErrorResponse from(ErrorCode errorCode, Map<String, Object> details, Exception ex) {
        return new ErrorResponse(
                Instant.now(),
                errorCode.name(),
                errorCode.getMessage(),
                details,
                ex.getClass().getSimpleName(),
                errorCode.getHttpStatus().value()
        );
    }

    // 일반 예외 처리용
    public static ErrorResponse from(HttpStatus status, Exception ex) {
        return new ErrorResponse(
                Instant.now(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                new HashMap<>(),
                ex.getClass().getSimpleName(),
                status.value()
        );
    }
}
