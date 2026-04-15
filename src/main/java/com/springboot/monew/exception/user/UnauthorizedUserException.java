package com.springboot.monew.exception.user;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class UnauthorizedUserException extends UserException {
    public UnauthorizedUserException(UUID userId) {
        super(ErrorCode.UNAUTHORIZED_USER, Map.of("userId", userId));
    }
}
