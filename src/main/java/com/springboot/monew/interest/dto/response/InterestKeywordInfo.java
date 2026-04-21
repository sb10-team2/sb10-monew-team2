package com.springboot.monew.interest.dto.response;

import java.util.UUID;

public record InterestKeywordInfo(
    UUID interestId,
    String keywordName
){

}
