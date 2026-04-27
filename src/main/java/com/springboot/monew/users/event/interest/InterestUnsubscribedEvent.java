package com.springboot.monew.users.event.interest;

import java.util.UUID;

public record InterestUnsubscribedEvent(
    UUID userId,
    UUID interestId
) {

}
