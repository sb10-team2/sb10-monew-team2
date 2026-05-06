package com.springboot.monew.user.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.user.dto.request.UserLoginRequest;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.request.UserUpdateRequest;
import com.springboot.monew.user.dto.response.UserDto;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class UserIntegrationTest extends BaseIntegrationsTest {

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
    // 회원가입과 닉네임 수정은 사용자 활동 문서와 Outbox도 함께 건드리므로 관련 저장소를 같이 비운다.
    // 각 테스트가 독립적으로 실행되도록 사용자 관련 데이터를 모두 초기화한다.
    userActivityRepository.deleteAll();
    userActivityOutboxRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("회원가입 요청이 유효하면 201 상태코드와 생성된 사용자 정보를 반환한다")
  void register_Returns201_WhenValidRequest() {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "register@test.com",
        "registerUser",
        "password123!"
    );

    // when
    // 실제 HTTP 요청으로 컨트롤러, 서비스, 저장소까지 한 번에 검증한다.
    ResponseEntity<UserDto> response = restTemplate.postForEntity(
        "/api/users",
        jsonEntity(request),
        UserDto.class
    );

    // then
    // 응답 본문은 생성된 사용자의 핵심 정보를 담아야 한다.
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().email()).isEqualTo(request.email());
    assertThat(response.getBody().nickname()).isEqualTo(request.nickname());
    assertThat(response.getBody().createdAt()).isNotNull();
    // 회원가입 성공 시 201과 Location 헤더를 반환해야 한다.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/users/" + response.getBody().id());
    // DB에도 실제로 저장되어야 회원가입 플로우가 끝난 것으로 본다.
    assertThat(userRepository.findById(response.getBody().id())).isPresent();
  }

  @Test
  @DisplayName("회원가입 요청 시 이메일이 중복되면 409 상태코드와 예외 응답을 반환한다")
  void register_Returns409_WhenEmailDuplicated() {
    // given
    userRepository.save(new User("duplicate@test.com", "existingUser", "password123!"));
    UserRegisterRequest request = new UserRegisterRequest(
        "duplicate@test.com",
        "newUser",
        "password123!"
    );

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "/api/users",
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    // 이메일 중복은 UserService에서 DUPLICATE_EMAIL 예외를 던지므로 UR01을 기대한다.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR01");
    assertThat(response.getBody().getDetails()).containsEntry("email", request.email());
  }

  @Test
  @DisplayName("회원가입 요청 시 닉네임이 중복되면 409 상태코드와 예외 응답을 반환한다")
  void register_Returns409_WhenNicknameDuplicated() {
    // given
    // 닉네임 중복은 이메일 중복과 별개 분기이므로 독립적으로 검증한다.
    userRepository.save(new User("existing@test.com", "duplicateNickname", "password123!"));
    UserRegisterRequest request = new UserRegisterRequest(
        "new@test.com",
        "duplicateNickname",
        "password123!"
    );

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "/api/users",
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR02");
    assertThat(response.getBody().getDetails()).containsEntry("nickname", request.nickname());
  }

  @Test
  @DisplayName("로그인 요청이 유효하면 200 상태코드와 사용자 정보를 반환한다")
  void login_Returns200_WhenValidRequest() {
    // given
    User savedUser = userRepository.save(new User("login@test.com", "loginUser", "password123!"));
    UserLoginRequest request = new UserLoginRequest("login@test.com", "password123!");

    // when
    ResponseEntity<UserDto> response = restTemplate.postForEntity(
        "/api/users/login",
        jsonEntity(request),
        UserDto.class
    );

    // then
    // 로그인 성공 시 저장된 사용자의 식별 정보가 그대로 반환되어야 한다.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(savedUser.getId());
    assertThat(response.getBody().email()).isEqualTo(savedUser.getEmail());
    assertThat(response.getBody().nickname()).isEqualTo(savedUser.getNickname());
  }

  @Test
  @DisplayName("로그인 요청 시 비밀번호가 일치하지 않으면 401 상태코드와 예외 응답을 반환한다")
  void login_Returns401_WhenPasswordInvalid() {
    // given
    userRepository.save(new User("login-fail@test.com", "loginFailUser", "password123!"));
    UserLoginRequest request = new UserLoginRequest("login-fail@test.com", "wrong-password");

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "/api/users/login",
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR04");
    assertThat(response.getBody().getDetails()).containsEntry("email", request.email());
  }

  @Test
  @DisplayName("로그인 요청 시 사용자가 없으면 404 상태코드와 예외 응답을 반환한다")
  void login_Returns404_WhenUserNotFound() {
    // given
    UserLoginRequest request = new UserLoginRequest("missing@test.com", "password123!");

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "/api/users/login",
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    // 존재하지 않는 이메일은 USER_NOT_FOUND로 처리된다.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR03");
    assertThat(response.getBody().getDetails()).containsEntry("email", request.email());
  }

  @Test
  @DisplayName("소프트 삭제된 사용자는 로그인 요청 시 404 상태코드와 예외 응답을 반환한다")
  void login_Returns404_WhenUserSoftDeleted() {
    // given
    // 소프트 삭제된 사용자는 조회되더라도 로그인에서는 USER_NOT_FOUND로 취급한다.
    User deletedUser = userRepository.save(
        new User("deleted-login@test.com", "deletedLoginUser", "password123!"));
    deletedUser.delete();
    userRepository.save(deletedUser);
    UserLoginRequest request = new UserLoginRequest("deleted-login@test.com", "password123!");

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
        "/api/users/login",
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR03");
    assertThat(response.getBody().getDetails()).containsEntry("email", request.email());
  }

  @Test
  @DisplayName("사용자 수정 요청이 유효하면 200 상태코드와 수정된 사용자 정보를 반환한다")
  void update_Returns200_WhenValidRequest() {
    // given
    User savedUser = userRepository.save(new User("update@test.com", "beforeNickname", "password123!"));
    UserUpdateRequest request = new UserUpdateRequest("afterNickname");

    // when
    // 현재 구현은 요청 헤더 권한 검증 없이 path의 userId 기준으로 수정한다.
    ResponseEntity<UserDto> response = restTemplate.exchange(
        "/api/users/" + savedUser.getId(),
        HttpMethod.PATCH,
        jsonEntity(request),
        UserDto.class
    );

    // then
    // 응답과 DB 값이 모두 변경되어야 수정 성공으로 본다.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(savedUser.getId());
    assertThat(response.getBody().nickname()).isEqualTo(request.nickname());
    assertThat(userRepository.findById(savedUser.getId())).isPresent()
        .get()
        .extracting(User::getNickname)
        .isEqualTo(request.nickname());
  }

  @Test
  @DisplayName("사용자 수정 요청 시 닉네임이 중복되면 409 상태코드와 예외 응답을 반환한다")
  void update_Returns409_WhenNicknameDuplicated() {
    // given
    User savedUser = userRepository.save(new User("update-1@test.com", "beforeNickname", "password123!"));
    userRepository.save(new User("update-2@test.com", "duplicatedNickname", "password123!"));
    UserUpdateRequest request = new UserUpdateRequest("duplicatedNickname");

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/users/" + savedUser.getId(),
        HttpMethod.PATCH,
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR02");
    assertThat(response.getBody().getDetails()).containsEntry("nickname", request.nickname());
  }

  @Test
  @DisplayName("사용자 수정 요청 시 사용자가 없으면 404 상태코드와 예외 응답을 반환한다")
  void update_Returns404_WhenUserNotFound() {
    // given
    UUID missingUserId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("afterNickname");

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/users/" + missingUserId,
        HttpMethod.PATCH,
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR03");
    assertThat(response.getBody().getDetails()).containsEntry("userId", missingUserId.toString());
  }

  @Test
  @DisplayName("소프트 삭제된 사용자는 수정 요청 시 404 상태코드와 예외 응답을 반환한다")
  void update_Returns404_WhenUserSoftDeleted() {
    // given
    User deletedUser = userRepository.save(
        new User("deleted-update@test.com", "deletedUpdateUser", "password123!"));
    deletedUser.delete();
    userRepository.save(deletedUser);
    UserUpdateRequest request = new UserUpdateRequest("afterNickname");

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/users/" + deletedUser.getId(),
        HttpMethod.PATCH,
        jsonEntity(request),
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR03");
    assertThat(response.getBody().getDetails())
        .containsEntry("userId", deletedUser.getId().toString());
  }

  @Test
  @DisplayName("사용자 소프트 삭제 요청이 유효하면 204 상태코드를 반환하고 deleted 상태가 된다")
  void delete_Returns204_WhenValidRequest() {
    // given
    User savedUser = userRepository.save(new User("delete@test.com", "deleteUser", "password123!"));

    // when
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/users/" + savedUser.getId(),
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        Void.class
    );

    // then
    // 소프트 삭제는 레코드를 남겨 둔 채 deletedAt만 설정되어야 한다.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(userRepository.findById(savedUser.getId())).isPresent()
        .get()
        .extracting(User::isDeleted)
        .isEqualTo(true);
  }

  @Test
  @DisplayName("사용자 소프트 삭제 요청 시 사용자가 없으면 404 상태코드와 예외 응답을 반환한다")
  void delete_Returns404_WhenUserNotFound() {
    // given
    UUID missingUserId = UUID.randomUUID();

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/users/" + missingUserId,
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR03");
    assertThat(response.getBody().getDetails()).containsEntry("userId", missingUserId.toString());
  }

  @Test
  @DisplayName("사용자 하드 삭제 요청이 유효하면 204 상태코드를 반환하고 DB에서 완전히 삭제된다")
  void hardDelete_Returns204_WhenValidRequest() {
    // given
    User savedUser = userRepository.save(new User("hard-delete@test.com", "hardDeleteUser", "password123!"));

    // when
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/users/" + savedUser.getId() + "/hard",
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        Void.class
    );

    // then
    // 하드 삭제는 저장소 조회 결과가 비어 있어야 한다.
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(userRepository.findById(savedUser.getId())).isEmpty();
  }

  @Test
  @DisplayName("사용자 하드 삭제 요청 시 사용자가 없으면 404 상태코드와 예외 응답을 반환한다")
  void hardDelete_Returns404_WhenUserNotFound() {
    // given
    UUID missingUserId = UUID.randomUUID();

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/users/" + missingUserId + "/hard",
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("UR03");
    assertThat(response.getBody().getDetails()).containsEntry("userId", missingUserId.toString());
  }

  private <T> HttpEntity<T> jsonEntity(T body) {
    // 통합 테스트 전반에서 JSON 요청 생성 방식을 통일한다.
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }
}
