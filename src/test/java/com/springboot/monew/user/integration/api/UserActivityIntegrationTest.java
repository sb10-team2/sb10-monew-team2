package com.springboot.monew.user.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.dto.response.UserActivityDto;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.repository.UserActivityRepository;
import com.springboot.monew.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class UserActivityIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserActivityRepository userActivityRepository;

  @Autowired
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @BeforeEach
  void setUp() {
    // 사용자 활동 조회는 사용자와 Mongo 활동 문서를 함께 사용하므로 둘 다 초기화한다.
    userActivityRepository.deleteAll();
    userActivityOutboxRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("사용자 활동 내역 조회 요청 시 활동 문서가 있으면 200 상태코드와 활동 내역을 반환한다")
  void getUserActivity_Returns200_WhenActivityDocumentExists() {
    // given
    User user = userRepository.save(new User("activity@test.com", "activityUser", "password123!"));
    userActivityRepository.save(
        new UserActivityDocument(user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt()));

    // when
    ResponseEntity<UserActivityDto> response = restTemplate.exchange(
        "/api/user-activities/" + user.getId(),
        HttpMethod.GET,
        null,
        UserActivityDto.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(user.getId());
    assertThat(response.getBody().email()).isEqualTo(user.getEmail());
    assertThat(response.getBody().nickname()).isEqualTo(user.getNickname());
    assertThat(response.getBody().subscriptions()).isEmpty();
    assertThat(response.getBody().comments()).isEmpty();
    assertThat(response.getBody().commentLikes()).isEmpty();
    assertThat(response.getBody().articleViews()).isEmpty();
  }

  @Test
  @DisplayName("사용자 활동 내역 조회 요청 시 활동 문서가 없어도 200 상태코드와 빈 활동 내역을 반환한다")
  void getUserActivity_Returns200WithEmptyLists_WhenActivityDocumentMissing() {
    // given
    // 현재 구현은 사용자만 존재하면 Mongo 활동 문서가 없어도 빈 배열 DTO를 내려준다.
    User user = userRepository.save(new User("empty@test.com", "emptyUser", "password123!"));

    // when
    ResponseEntity<UserActivityDto> response = restTemplate.exchange(
        "/api/user-activities/" + user.getId(),
        HttpMethod.GET,
        null,
        UserActivityDto.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(user.getId());
    assertThat(response.getBody().email()).isEqualTo(user.getEmail());
    assertThat(response.getBody().nickname()).isEqualTo(user.getNickname());
    assertThat(response.getBody().subscriptions()).isEmpty();
    assertThat(response.getBody().comments()).isEmpty();
    assertThat(response.getBody().commentLikes()).isEmpty();
    assertThat(response.getBody().articleViews()).isEmpty();
  }

  @Test
  @DisplayName("사용자 활동 내역 조회 요청 시 사용자가 없으면 404 상태코드와 예외 응답을 반환한다")
  void getUserActivity_Returns404_WhenUserNotFound() {
    // given
    UUID missingUserId = UUID.randomUUID();

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/user-activities/" + missingUserId,
        HttpMethod.GET,
        null,
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR03");
    assertThat(response.getBody().getDetails()).containsEntry("userId", missingUserId.toString());
  }
}
