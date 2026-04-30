package com.springboot.monew.user.event.interest;

import java.util.List;
import java.util.UUID;

public record InterestUpdatedEvent(
    UUID interestId,
    List<String> keywords
) {

}
