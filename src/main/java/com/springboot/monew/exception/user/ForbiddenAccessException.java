package com.springboot.monew.exception.user;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class ForbiddenAccessException extends UserException {
    public ForbiddenAccessException(UUID userId) {
        super(ErrorCode.FORBIDDEN_ACCESS, Map.of("userId", userId));
    }
}
