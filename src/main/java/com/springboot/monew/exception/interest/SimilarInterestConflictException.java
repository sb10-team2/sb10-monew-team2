package com.springboot.monew.exception.interest;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class SimilarInterestConflictException extends InterestException {
    public SimilarInterestConflictException(String interestName) {
        super(ErrorCode.SIMILAR_INTEREST_CONFLICT, Map.of("interestName", interestName));
    }
}
