package com.springboot.monew.notification.exception;

import com.springboot.monew.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
  INVALID_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "N001", "유효한 데이터가 아닙니다"),
  ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "N002", "이미 확인한 알람입니다"),
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
