package com.springboot.monew.user.event.interest;

import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import java.util.UUID;

public record InterestSubscribedEvent(
    UUID userId,
    SubscriptionItem item
) {

}
