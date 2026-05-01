package com.springboot.monew.user.outbox.payload.interest;

import java.util.List;
import java.util.UUID;

public record InterestUpdatedPayload(
    UUID interestId,
    List<String> keywords
) {
  public static InterestUpdatedPayload of(UUID interestId, List<String> keywords) {
    return new InterestUpdatedPayload(interestId, keywords);
  }
}
