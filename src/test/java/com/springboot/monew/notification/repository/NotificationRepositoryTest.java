package com.springboot.monew.notification.repository;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

public class NotificationRepositoryTest extends BaseRepositoryTest {
    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        queryInspector.clear();
    }

    @Test
    @DisplayName("""
            관심사와 함께 알람 객체가 저장된다
            단일 객체 저장 시 쿼리 1번 생성된다
            조회 시 쿼리 1번 생성된다""")
    void successToCreateByInterest() {
        // given
        User user = testEntityManager.generateUser();
        Interest interest = testEntityManager.generateInterest();
        Notification expected = Notification.builder()
                .content("asd")
                .resourceType(ResourceType.INTEREST)
                .user(user)
                .interest(interest)
                .build();
        clear();

        // when
        notificationRepository.saveAndFlush(expected);
        printQueries();
        ensureQueryCount(1);
        clear();

        // then
        Notification actual = notificationRepository.findById(expected.getId()).orElseThrow();
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("user", "commentLike", "interest")
                .withEqualsForType(this::compareInstant, Instant.class)
                .isEqualTo(expected);

        Assertions.assertThat(actual.getUser())
                .usingRecursiveComparison()
                .withEqualsForType(this::compareInstant, Instant.class)
                .isEqualTo(user);

        Assertions.assertThat(actual.getInterest())
                .usingRecursiveComparison()
                .withEqualsForType(this::compareInstant, Instant.class)
                .isEqualTo(interest);

        ensureQueryCount(1);
        printQueries();
    }
}
