package com.springboot.monew.common.fixture;

import com.springboot.monew.interest.entity.Interest;

public final class InterestFixture {
    private static final BaseFixture baseFixture = BaseFixture.INSTANT;

    private InterestFixture() {
    }

    public static Interest createEntity() {
        return baseFixture.baseUpdatableEntity(Interest.class).create();
    }
}
