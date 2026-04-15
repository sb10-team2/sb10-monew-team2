package com.springboot.monew.exception.user;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class DuplicateEmailException extends UserException {
    public DuplicateEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, Map.of("email", email));
    }
}
