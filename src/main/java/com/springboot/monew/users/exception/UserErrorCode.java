package com.springboot.monew.users.exception;

import com.springboot.monew.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "UR01", "이미 사용 중인 이메일입니다."),
  DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "UR02", "이미 사용 중인 닉네임입니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "UR03", "사용자를 찾을 수 없습니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "UR04", "이메일 또는 비밀번호가 올바르지 않습니다."),
  USER_NOT_OWNED(HttpStatus.FORBIDDEN, "UR05", "본인의 사용자 정보만 수정할 수 있습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
