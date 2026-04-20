package com.springboot.monew.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.dto.UserRegisterRequest;
import com.springboot.monew.users.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

        // Mockito.when을 클래스명 없이 간단히 사용하기 위해 static import를 하였음.
        when(userService.register(ArgumentMatchers.any(UserRegisterRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + userId))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("monew123"))
                .andExpect(jsonPath("$.createdAt").value("2026-04-17T01:46:03.003Z"));

        // 컨트롤러가 실제로 register을 호출했는지 검증하기 위해 추가
        verify(userService).register(ArgumentMatchers.any(UserRegisterRequest.class));
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
        verify(userService, never()).register(ArgumentMatchers.any(UserRegisterRequest.class));
    }
}
