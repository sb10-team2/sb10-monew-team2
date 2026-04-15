package com.springboot.monew.exception.interest;

import com.springboot.monew.exception.ErrorCode;
import com.springboot.monew.exception.MonewException;

import java.util.Map;

public abstract class InterestException extends MonewException {
    public InterestException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
