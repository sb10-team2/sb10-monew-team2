package com.springboot.monew.notification.exception;

import com.springboot.monew.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  // 5xx
  INVALID_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "NT01", "DB에 저장된 알림 데이터가 손상되었거나 유효하지 않습니다."),
  DOMAIN_CONSTRAINT_VIOLATED(HttpStatus.INTERNAL_SERVER_ERROR, "NT02",
      "알림 타입(ResourceType)과 연결된 객체가 일치하지 않습니다."),
  MISSING_REQUIRED_FIELD(HttpStatus.INTERNAL_SERVER_ERROR, "NT03", "알림 생성에 필요한 필수 객체가 누락되었습니다."),
  NOTIFICATION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "NT05", "알림을 찾을 수 없습니다."),

  // 4xx
  ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "NT04", "이미 읽음 처리된 알림입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
