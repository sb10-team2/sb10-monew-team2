package com.springboot.monew.common.fixture;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.common.entity.BaseUpdatableEntity;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import java.util.List;

import static org.instancio.Select.field;

public enum BaseFixture {
    INSTANT;

    public <T extends BaseEntity> InstancioApi<T> baseEntity(Class<T> type) {
        return Instancio.of(type)
                .ignore(field(BaseEntity::getCreatedAt))
                .ignore(field(BaseEntity::getId));
    }

    public <T extends BaseEntity> InstancioApi<List<T>> baseEntities(Class<T> type) {
        return Instancio.ofList(type)
                .ignore(field(BaseEntity::getCreatedAt))
                .ignore(field(BaseEntity::getId));
    }

    public <T extends BaseUpdatableEntity> InstancioApi<T> baseUpdatableEntity(Class<T> type) {
        return Instancio.of(type)
                .ignore(field(BaseUpdatableEntity::getCreatedAt))
                .ignore(field(BaseUpdatableEntity::getUpdatedAt))
                .ignore(field(BaseEntity::getId));
    }

    public <T extends BaseUpdatableEntity> InstancioApi<List<T>> baseUpdatableEntities(
            Class<T> type) {
        return Instancio.ofList(type)
                .ignore(field(BaseUpdatableEntity::getCreatedAt))
                .ignore(field(BaseUpdatableEntity::getUpdatedAt))
                .ignore(field(BaseEntity::getId));
    }
}
