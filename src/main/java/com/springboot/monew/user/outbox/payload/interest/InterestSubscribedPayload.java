package com.springboot.monew.user.outbox.payload.interest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterestSubscribedPayload(
    UUID userId,
    UUID subscriptionId,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    Instant createdAt
) {

}
