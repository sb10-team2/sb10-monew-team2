package com.springboot.monew.exception.interest;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class InterestNotFoundException extends InterestException {
    public InterestNotFoundException(UUID interestId) {
        super(ErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId));
    }
}
