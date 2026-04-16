package com.springboot.monew.fixture;

import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public final class NotificationsFixture {
    private NotificationsFixture() {
    }

    public static Notification createEntityWithInterest() {
        return Instancio.of(Notification.class)
                .ignore(field(Notification::getCommentLike))
                .set(field(Notification::getResourceType), ResourceType.INTEREST)
                .create();
    }

    public static Notification createEntityWithCommentLike() {
        return Instancio.of(Notification.class)
                .ignore(field(Notification::getInterest))
                .set(field(Notification::getResourceType), ResourceType.COMMENT)
                .create();
    }
}
