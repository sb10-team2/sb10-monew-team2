package com.springboot.monew.common.exception;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class MonewException extends RuntimeException {

  private final Instant timestamp;
  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  public MonewException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = details;
  }

  public MonewException(ErrorCode errorCode, UUID id) {
    super(errorCode.getMessage());
    this.timestamp = Instant.now();
    this.errorCode = errorCode;
    this.details = Map.of(getClass().getSimpleName(), id);
  }
}
