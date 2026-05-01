package com.springboot.monew.user.outbox.payload.interest;

import com.springboot.monew.user.event.interest.InterestUnsubscribedEvent;
import java.util.UUID;

public record InterestUnsubscribedPayload(
    UUID userId,
    UUID interestId
) {
  public static InterestUnsubscribedPayload of(UUID userId, UUID interestId) {
    return new InterestUnsubscribedPayload(userId, interestId);
  }

  public static InterestUnsubscribedPayload of(InterestUnsubscribedEvent event) {
    return new InterestUnsubscribedPayload(event.userId(), event.interestId());
  }
}
