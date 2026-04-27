package com.springboot.monew.users.event.interest;

import com.springboot.monew.users.document.UserActivityDocument.SubscriptionItem;
import java.util.UUID;

public record InterestSubscribedEvent(
    UUID userId,
    SubscriptionItem item
) {

}
