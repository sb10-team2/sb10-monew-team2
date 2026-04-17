package com.springboot.monew.interest.exception;

import com.springboot.monew.exception.ErrorCode;
import com.springboot.monew.exception.MonewException;

import java.util.Map;

public class InterestException extends MonewException {
    public InterestException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
