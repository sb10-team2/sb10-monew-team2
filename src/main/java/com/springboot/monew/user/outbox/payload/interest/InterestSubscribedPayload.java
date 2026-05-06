package com.springboot.monew.user.outbox.payload.interest;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.Subscription;
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
  public static InterestSubscribedPayload of(Subscription subscription, List<String> keywords) {
    Interest interest = subscription.getInterest();

    return new InterestSubscribedPayload(
        subscription.getUser().getId(),
        subscription.getId(),
        interest.getId(),
        interest.getName(),
        keywords,
        subscription.getCreatedAt()
    );
  }
}
