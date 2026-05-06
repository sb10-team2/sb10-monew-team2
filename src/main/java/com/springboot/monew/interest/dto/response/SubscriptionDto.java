package com.springboot.monew.interest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// 구독 응답 dto
public record SubscriptionDto(
    UUID id,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    Long interestSubscriberCount,
    Instant createdAt
) {

}
