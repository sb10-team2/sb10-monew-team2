package com.springboot.monew.notification.exception;

import com.springboot.monew.common.exception.ErrorCode;
import com.springboot.monew.common.exception.MonewException;

import java.util.Map;

public class NotificationException extends MonewException {
    public NotificationException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
