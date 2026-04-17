package com.springboot.monew.common.exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // MonewException: ErrorCode에 정의된 모든 커스텀 예외 통합 처리
    // 5xx면 error, 나머지는 warn으로 로그 레벨 구분
    @ExceptionHandler(MonewException.class)
    public ResponseEntity<ErrorResponse> handleMonewException(MonewException ex) {
        HttpStatus status = ex.getErrorCode().getHttpStatus();
        if (status.is5xxServerError()) {
            log.error("[{}] {} - details: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getDetails(), ex);
        } else {
            log.warn("[{}] {} - details: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getDetails());
        }
        return ResponseEntity.status(status).body(ErrorResponse.from(ex));
    }

    // @Valid 검증 실패 시 발생
    // BindingResult에서 필드별 에러 메시지를 Map으로 변환하여 details에 담아 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 동일 필드에 여러 검증 에러 발생 시 List로 수집 (중복 키 IllegalStateException 방지)
        Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(
                                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "유효하지 않은 값입니다",
                                Collectors.toList()
                        )
                ));
        Map<String, Object> details = new java.util.HashMap<>(fieldErrors);
        log.warn("[MethodArgumentNotValidException] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(HttpStatus.BAD_REQUEST, details, ex));
    }

    // 클라이언트 요청 오류
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[IllegalArgumentException] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(HttpStatus.BAD_REQUEST, ex));
    }

    // 클라이언트가 지원하지 않는 Content-Type으로 요청한 경우
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("[HttpMediaTypeNotSupportedException] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.from(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex));
    }

    // 위에서 처리되지 않은 모든 예외 처리 (예상치 못한 서버 오류)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("[Exception] 예상치 못한 오류 발생 ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.from(HttpStatus.INTERNAL_SERVER_ERROR, ex));
    }
}
