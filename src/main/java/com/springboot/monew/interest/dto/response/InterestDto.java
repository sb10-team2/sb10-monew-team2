package com.springboot.monew.interest.dto.response;

import java.util.List;
import java.util.UUID;

// 관심사 응답 dto
public record InterestDto(
    UUID id,
    String name,
    List<String> keywords,
    long subscriberCount,
    boolean subscribedByMe
) {

}
