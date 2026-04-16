package com.springboot.monew.exception.comment;

import com.springboot.monew.exception.ErrorCode;
import com.springboot.monew.exception.MonewException;

import java.util.Map;

public class CommentException extends MonewException {
    public CommentException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
