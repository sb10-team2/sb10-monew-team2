package com.springboot.monew.fixture;

import com.springboot.monew.entity.Interest;
import org.instancio.Instancio;

public final class InterestFixture {
    private InterestFixture() {
    }

    public static Interest createEntity() {
        return Instancio.create(Interest.class);
    }
}
