package com.springboot.monew.notification.exception;

import com.springboot.monew.common.exception.ErrorCode;
import com.springboot.monew.common.exception.MonewException;
import java.util.Map;
import java.util.UUID;

public class NotificationException extends MonewException {

  public NotificationException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public NotificationException(ErrorCode errorCode, UUID id) {
    super(errorCode, id);
  }

  public NotificationException(ErrorCode errorCode) {
    this(errorCode, Map.of());
  }

  public NotificationException(ErrorCode errorCode, String detailMessage) {
    this(errorCode, Map.of("detailMessage", detailMessage));
  }
}
