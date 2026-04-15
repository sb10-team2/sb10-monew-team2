package com.springboot.monew.exception.user;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class InvalidCredentialsException extends UserException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, Map.of());
    }
}
