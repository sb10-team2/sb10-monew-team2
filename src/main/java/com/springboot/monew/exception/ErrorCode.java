package com.springboot.monew.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // User
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_EMAIL_LENGTH(HttpStatus.BAD_REQUEST, "이메일 길이가 올바르지 않습니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "닉네임 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    MISSING_AUTH_HEADER(HttpStatus.UNAUTHORIZED, "인증 헤더가 누락되었습니다."),
    INVALID_USER_ID(HttpStatus.UNAUTHORIZED, "유효하지 않은 사용자 ID입니다."),
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Interest
    SIMILAR_INTEREST_CONFLICT(HttpStatus.CONFLICT, "유사한 관심사가 이미 존재합니다."),
    INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심사를 찾을 수 없습니다."),

    // Article
    INVALID_ARTICLE_REQUEST(HttpStatus.BAD_REQUEST, "기사 요청 데이터가 올바르지 않습니다."),
    INVALID_SORT_CONDITION(HttpStatus.BAD_REQUEST, "정렬 조건이 올바르지 않습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "커서 값이 올바르지 않습니다."),
    DUPLICATE_ARTICLE(HttpStatus.CONFLICT, "이미 존재하는 기사입니다."),
    ARTICLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 기사입니다."),
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "기사를 찾을 수 없습니다."),

    // Comment
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "유효성 검사에 실패했습니다."),
    COMMENT_NOT_OWNED_BY_USER(HttpStatus.FORBIDDEN, "해당 댓글의 작성자가 아닙니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요한 댓글입니다."),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.CONFLICT, "좋아요 정보를 찾을 수 없습니다."),

    // Backup
    BACKUP_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "백업 업로드에 실패했습니다."),
    BACKUP_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "백업 직렬화에 실패했습니다."),
    BACKUP_NOT_FOUND(HttpStatus.NOT_FOUND, "백업을 찾을 수 없습니다."),
    RECOVERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "복구에 실패했습니다."),

    // Server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
