package com.springboot.monew.common.fixture;

import com.springboot.monew.users.entity.User;

public final class UserFixture {
    private static final BaseFixture baseFixture = BaseFixture.INSTANT;

    private UserFixture() {
    }

    public static User createEntity() {
        return baseFixture.baseUpdatableEntity(User.class).create();
    }
}
