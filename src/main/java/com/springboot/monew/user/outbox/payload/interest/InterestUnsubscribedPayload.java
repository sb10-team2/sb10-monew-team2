package com.springboot.monew.user.outbox.payload.interest;

import java.util.UUID;

public record InterestUnsubscribedPayload(
    UUID userId,
    UUID interestId
) {
  public static InterestUnsubscribedPayload of(UUID userId, UUID interestId) {
    return new InterestUnsubscribedPayload(userId, interestId);
  }
}
