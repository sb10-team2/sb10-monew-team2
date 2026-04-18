package com.springboot.monew.users.service;

import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.dto.UserLoginRequest;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.mapper.UserMapper;
import com.springboot.monew.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("정상 로그인 시 사용자 정보를 반환한다")
    void login_success() {

        // given
        UserLoginRequest request = new UserLoginRequest("test@example.com", "password123");
        User user = new User("test@example.com", "monew123", "password123");

        UserDto expected = new UserDto(
                UUID.randomUUID(),
                "test@example.com",
                "monew123",
                Instant.parse("2026-04-18T00:00:00Z")
        );

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // when
        UserDto result = userService.login(request);

        // then
        assertThat(result).isEqualTo(expected);
        verify(userRepository).findByEmail("test@example.com");
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인하면 USER_NOT_FOUND 예외가 발생한다")
    void login_userNotFound() {

        // given
        UserLoginRequest request = new UserLoginRequest("missing@example.com", "password123");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("email", "missing@example.com"));
                });
        verify(userRepository).findByEmail("missing@example.com");
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외가 발생한다")
    void login_invalidPassword() {

        // given
        UserLoginRequest request = new UserLoginRequest("test@example.com", "wrong-password");
        User user = new User("test@example.com", "monew123", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("email", "test@example.com"));
                });
        verify(userRepository).findByEmail("test@example.com");
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("삭제된 사용자는 로그인할 수 없고 USER_NOT_FOUND 예외가 발생한다")
    void login_deletedUser() {

        // given
        UserLoginRequest request = new UserLoginRequest("deleted@example.com", "password123");
        User deletedUser = new User("deleted@example.com", "monew123", "password123");
        deletedUser.delete();

        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.of(deletedUser));

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("email", "deleted@example.com"));
                });
        verify(userRepository).findByEmail("deleted@example.com");
        verify(userMapper, never()).toDto(any());
    }
}
