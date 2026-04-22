package com.springboot.monew.comment.exception;

import com.springboot.monew.common.exception.ErrorCode;
import com.springboot.monew.common.exception.MonewException;
import java.util.Map;
import java.util.UUID;

public class CommentException extends MonewException {

  public CommentException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public CommentException(ErrorCode errorCode, UUID id) {
    super(errorCode, id);
  }
}
