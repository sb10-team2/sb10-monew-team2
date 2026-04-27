package com.springboot.monew.users.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springboot.monew.users.dto.response.UserActivityDto;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.service.UserActivityService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserActivityController.class)
public class UserActivityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserActivityService userActivityService;

  @Test
  @DisplayName("사용자 활동 내역 조회 API는 200 OK와 활동 내역을 반환한다")
  void getUserActivity_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-18T00:00:00Z");

    UserActivityDto expected = new UserActivityDto(
        userId,
        "test@example.com",
        "monew123",
        createdAt,
        List.of(),
        List.of(),
        List.of(),
        List.of()
    );

    given(userActivityService.findUserActivity(userId)).willReturn(expected);

    // when & then
    mockMvc.perform(get("/api/user-activities/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value(expected.email()))
        .andExpect(jsonPath("$.nickname").value(expected.nickname()))
        .andExpect(jsonPath("$.createdAt").value("2026-04-18T00:00:00Z"))
        .andExpect(jsonPath("$.subscriptions").isArray())
        .andExpect(jsonPath("$.comments").isArray())
        .andExpect(jsonPath("$.commentLikes").isArray())
        .andExpect(jsonPath("$.articleViews").isArray());

    verify(userActivityService).findUserActivity(userId);
  }

  @Test
  @DisplayName("사용자 활동 내역 조회 API는 사용자가 없으면 404 Not Found를 반환한다")
  void getUserActivity_userNotFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    given(userActivityService.findUserActivity(userId))
        .willThrow(new UserException(
            UserErrorCode.USER_NOT_FOUND,
            Map.of("userId", userId)
        ));

    // when & then
    mockMvc.perform(get("/api/user-activities/{userId}", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("UR03"))
        .andExpect(jsonPath("$.exceptionType").value("UserException"))
        .andExpect(jsonPath("$.details.userId").value(userId.toString()));

    verify(userActivityService).findUserActivity(userId);
  }

}
