package com.springboot.monew.users.controller;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.users.dto.request.UserLoginRequest;
import com.springboot.monew.users.dto.request.UserRegisterRequest;
import com.springboot.monew.users.dto.request.UserUpdateRequest;
import com.springboot.monew.users.dto.response.UserDto;
import com.springboot.monew.users.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @Test
  @DisplayName("정상 회원가입 요청 시 201 Created와 사용자 정보를 반환한다")
  void register_success() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@example.com",
        "monew123",
        "ab12!@"
    );

    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-17T01:46:03.003Z");

    UserDto response = new UserDto(
        userId,
        "test@example.com",
        "monew123",
        createdAt
    );

    given(userService.register(any(UserRegisterRequest.class))).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/users/" + userId))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.nickname").value("monew123"))
        .andExpect(jsonPath("$.createdAt").value("2026-04-17T01:46:03.003Z"));

    //ArgumentCaptor로 서비스에 전달된 UserRegisterRequest 값을 잡아서 email, nickname, password가 요청값과 같은지 검증
    ArgumentCaptor<UserRegisterRequest> captor =
        ArgumentCaptor.forClass(UserRegisterRequest.class);

    // userService.register()이 호출되었는지 확인하고, 전달된 요청 객체를 가져와 검증
    verify(userService).register(captor.capture());

    // 컨트롤러가 요청 본문을 UserRegisterRequest로 올바르게 파싱해 서비스에 전달했는지 검증
    assertThat(captor.getValue().email()).isEqualTo(request.email());
    assertThat(captor.getValue().nickname()).isEqualTo(request.nickname());
    assertThat(captor.getValue().password()).isEqualTo(request.password());
  }

  @Test
  @DisplayName("유효하지 않은 회원가입 요청이면 400 Bad Request를 반환한다")
  void register_validationFail() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "invalid-email",
        "",
        "1234"
    );

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.email").isArray())
        .andExpect(jsonPath("$.details.nickname").isArray())
        .andExpect(jsonPath("$.details.password").isArray());

    // 컨트롤러가 실제로 register을 호출을 안했는지 검증하기 위해 추가
    verify(userService, never()).register(any(UserRegisterRequest.class));
  }

  @Test
  @DisplayName("정상 로그인 요청이면 200 OK와 사용자 정보를 반환한다")
  void login_success() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "test@example.com",
        "password123"
    );

    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-17T01:46:03.003Z");

    UserDto expected = new UserDto(
        userId,
        "test@example.com",
        "monew123",
        createdAt
    );

    given(userService.login(any(UserLoginRequest.class))).willReturn(expected);

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(expected.email()))
        .andExpect(jsonPath("$.nickname").value(expected.nickname()))
        .andExpect(jsonPath("$.createdAt").value("2026-04-17T01:46:03.003Z"));

    // ArgumentCaptor로 서비스에 전달된 UserLoginRequest 값을 잡아서 email, password가 요청값과 같은지 검증
    ArgumentCaptor<UserLoginRequest> captor =
        ArgumentCaptor.forClass(UserLoginRequest.class);

    // userService.login()이 호출되었는지 확인하고, 전달된 요청 객체를 가져와 검증
    verify(userService).login(captor.capture());

    // // 컨트롤러가 요청 본문을 UserLoginRequest로 올바르게 파싱해 서비스에 전달했는지 검증
    assertThat(captor.getValue().email()).isEqualTo(request.email());
    assertThat(captor.getValue().password()).isEqualTo(request.password());
  }

  @Test
  @DisplayName("유효하지 않은 로그인 요청이면 400 Bad Request를 반환한다")
  void login_validationFail() throws Exception {
    //given
    UserLoginRequest request = new UserLoginRequest(
        "invalid-email",
        ""
    );

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.email").isArray())
        .andExpect(jsonPath("$.details.password").isArray());

    verify(userService, never()).login(any(UserLoginRequest.class));
  }

  @Test
  @DisplayName("정상 사용자 정보 수정 요청이면 200 OK와 수정된 사용자 정보를 반환한다")
  void update_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    UserUpdateRequest request = new UserUpdateRequest("newNickname");
    Instant createdAt = Instant.parse("2026-04-17T01:46:03.003Z");

    UserDto expected = new UserDto(
        userId,
        "test@example.com",
        "monew123",
        createdAt
    );

    given(userService.update(eq(userId), any(UserUpdateRequest.class))).willReturn(
        expected);

    // when & then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .header("MoNew-Request-User-ID", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value(expected.email()))
        .andExpect(jsonPath("$.nickname").value(expected.nickname()))
        .andExpect(jsonPath("$.createdAt").value("2026-04-17T01:46:03.003Z"));

    // 서비스에 전달된 UserUpdateRequest를 가져와 nickname이 요청값과 같은지 검증
    ArgumentCaptor<UserUpdateRequest> captor =
        ArgumentCaptor.forClass(UserUpdateRequest.class);

    // userService.update()가 호출되었는지 확인하고, 전달된 요청 객체를 가져와 검증
    verify(userService).update(eq(userId), captor.capture());

    // 컨트롤러가 요청 본문을 UserUpdateRequest로 올바르게 파싱해 서비스에 전달했는지 검증
    assertThat(captor.getValue().nickname()).isEqualTo(request.nickname());
  }

  @Test
  @DisplayName("유효하지 않은 사용자 정보 수정 요청이면 400 Bad Request를 반환한다")
  void update_validationFail() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    UserUpdateRequest request = new UserUpdateRequest("");

    // when & then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .header("MoNew-Request-User-ID", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.nickname").isArray());

    verify(userService, never()).update(eq(userId), any(UserUpdateRequest.class));
  }

  @Test
  @DisplayName("정상 사용자 논리 삭제 요청이면 204 No Content를 반환한다")
  void delete_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNoContent());

    verify(userService).delete(userId);
  }

  @Test
  @DisplayName("정상 사용자 물리 삭제 요청이면 204 No Content를 반환한다")
  void hardDelete_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/users/{userId}/hard", userId))
        .andExpect(status().isNoContent());

    verify(userService).hardDelete(userId);
  }
}
