package com.springboot.monew.user.event.interest;

import java.util.UUID;

public record InterestUnsubscribedEvent(
    UUID userId,
    UUID interestId
) {

}
