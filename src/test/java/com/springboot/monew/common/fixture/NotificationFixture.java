package com.springboot.monew.common.fixture;

import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;

import static org.instancio.Select.field;

public final class NotificationFixture {
    private static final BaseFixture baseFixture = BaseFixture.INSTANT;

    private NotificationFixture() {
    }

    public static Notification createEntityWithInterest() {
        return baseFixture.baseEntity(Notification.class)
                .ignore(field(Notification::getCommentLike))
                .set(field(Notification::getResourceType), ResourceType.INTEREST)
                .create();
    }

    public static Notification createEntityWithCommentLike() {
        return baseFixture.baseEntity(Notification.class)
                .ignore(field(Notification::getInterest))
                .set(field(Notification::getResourceType), ResourceType.COMMENT)
                .create();
    }
}
