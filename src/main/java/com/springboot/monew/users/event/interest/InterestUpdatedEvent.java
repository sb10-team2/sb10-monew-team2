package com.springboot.monew.users.event.interest;

import java.util.List;
import java.util.UUID;

public record InterestUpdatedEvent(
    UUID interestId,
    List<String> keywords
) {

}
