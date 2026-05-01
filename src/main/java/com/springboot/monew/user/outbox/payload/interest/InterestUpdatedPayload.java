package com.springboot.monew.user.outbox.payload.interest;

import com.springboot.monew.user.event.interest.InterestUpdatedEvent;
import java.util.List;
import java.util.UUID;

public record InterestUpdatedPayload(
    UUID interestId,
    List<String> keywords
) {
  public static InterestUpdatedPayload of(UUID interestId, List<String> keywords) {
    return new InterestUpdatedPayload(interestId, keywords);
  }

  public static InterestUpdatedPayload of(InterestUpdatedEvent event) {
    return new InterestUpdatedPayload(event.interestId(), event.keywords());
  }
}
