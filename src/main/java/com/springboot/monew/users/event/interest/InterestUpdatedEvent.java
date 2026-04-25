package com.springboot.monew.users.event.interest;

import java.util.List;
import java.util.UUID;

public record InterestUpdatedEvent(
    UUID interestId,
    String interestName,
    List<String> keywords,
    long subscriberCount
) {

}
