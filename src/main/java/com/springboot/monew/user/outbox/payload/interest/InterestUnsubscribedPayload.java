package com.springboot.monew.user.outbox.payload.interest;

import java.util.UUID;

public record InterestUnsubscribedPayload(
    UUID userId,
    UUID interestId
) {

}
