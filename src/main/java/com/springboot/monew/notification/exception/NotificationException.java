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
    this(errorCode, Map.of("id", id));
  }
}
