package com.springboot.monew.interest.entity;

import com.springboot.monew.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "interests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_INTERESTS_NAME",
                        columnNames = "name"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interest extends BaseUpdatableEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "subscriber_count", nullable = false)
    private long subscriberCount;

    public Interest(String name) {
        this.name = name;
        this.subscriberCount = 0;
    }

    // 구독자 수 증가
    public void increaseSubscriberCount() {
        this.subscriberCount++;
    }

    // 구독자 수 감소
    public void decreaseSubscriberCount() {
        if (this.subscriberCount > 0) {
            this.subscriberCount--;
        }
    }
}
