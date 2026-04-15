package com.springboot.monew.exception.user;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class UserNotFoundException extends UserException {
    public UserNotFoundException(UUID userId) {
        super(ErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
    public UserNotFoundException(String maskedEmail) {
        super(ErrorCode.USER_NOT_FOUND, Map.of("email", maskEmail(maskedEmail)));
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
