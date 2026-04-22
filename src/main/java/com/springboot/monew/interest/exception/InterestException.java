package com.springboot.monew.interest.exception;

import com.springboot.monew.common.exception.ErrorCode;
import com.springboot.monew.common.exception.MonewException;
import java.util.Map;
import java.util.UUID;

public class InterestException extends MonewException {

  public InterestException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public InterestException(ErrorCode errorCode, UUID id) {
    super(errorCode, id);
  }
}
