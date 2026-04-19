package com.springboot.monew.users.service;

import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.dto.UserLoginRequest;
import com.springboot.monew.users.dto.UserRegisterRequest;
import com.springboot.monew.users.dto.UserUpdateRequest;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
    @DisplayName("이메일이 중복되면 DUPLICATE_EMAIL 예외가 발생한다")
    void register_duplicateEmail() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@example.com",
                "monew123",
                "ab12!@"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // when & then
        /*
            초기에는 catchThrowableOfType을 사용했지만, 현재는 AssertJ에서 권장하는 방식인 assertThatThrownBy를 사용했다.
            catchThrowableOfType은 예외를 먼저 변수로 받아서 검증하는 구조라 코드가 분리되는 반면,
            assertThatThrownBy는 예외 발생과 검증을 하나의 체이닝 형태로 이어서 작성할 수 있어 가독성과 확장성이 더 좋다.
         */
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("email", request.email()));
                });
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).existsByNickname(anyString());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("닉네임이 중복되면 DUPLICATE_NICKNAME 예외가 발생한다")
    void register_duplicateNickname() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@example.com",
                "monew123",
                "ab12!@"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByNickname(request.nickname())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATE_NICKNAME);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("nickname", request.nickname()));
                });
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByNickname(request.nickname());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("회원가입 성공 시 사용자를 저장한 뒤 UserDto를 반환한다")
    void register_success() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "test@example.com",
                "monew123",
                "ab12!@"
        );

        User savedUser = new User("test@example.com", "monew123", "ab12!@");
        UserDto expected = new UserDto(
                UUID.randomUUID(),
                "test@example.com",
                "monew123",
                Instant.parse("2026-04-18T00:00:00Z")
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expected);

        // when
        UserDto result = userService.register(request);

        // then
        assertThat(result).isEqualTo(expected);
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByNickname(request.nickname());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDto(savedUser);
    }

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

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // when
        UserDto result = userService.login(request);

        // then
        assertThat(result).isEqualTo(expected);
        verify(userRepository).findByEmail(request.email());
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

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("email", request.email()));
                });
        verify(userRepository).findByEmail(request.email());
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

    @Test
    @DisplayName("닉네임 수정에 성공하면 수정된 사용자 정보를 반환한다")
    void update_success() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("newNickname");
        User user = new User("test@example.com",  "oldNickname", "password123");

        UserDto expected = new UserDto(
                userId,
                "test@example.com",
                "newNickname",
                Instant.parse("2026-04-18T00:00:00Z")
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname(request.nickname())).thenReturn(false);
        when(userMapper.toDto(user)).thenReturn(expected);

        // when
        UserDto result = userService.update(userId, userId, request);

        // then
        assertThat(result).isEqualTo(expected);
        assertThat(user.getNickname()).isEqualTo(request.nickname());
        verify(userRepository).findById(userId);
        verify(userRepository).existsByNickname(request.nickname());
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("요청 사용자와 수정 대상 사용자가 다르면 USER_NOT_OWNED 예외가 발생한다.")
    void update_userNotOwned() {
        // given
        UUID userId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("newNickname");

        // when & then
        assertThatThrownBy(() -> userService.update(userId, requestUserId, request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_OWNED);
                    assertThat(exception.getDetails()).isEqualTo(
                            Map.of("userId", userId, "requestUserId", requestUserId));
                });
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).existsByNickname(anyString());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 닉네임을 수정하면 USER_NOT_FOUND 예외가 발생한다")
    void update_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("newNickname");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.update(userId, userId, request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
                });
        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByNickname(anyString());
        verify(userMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 수정하면 DUPLICATE_NICKNAME 예외가 발생한다")
    void update_duplicateNickname() {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("duplicateNickname");
        User user = new User("test@example.com",  "oldNickname", "password123");

        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname(request.nickname())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.update(userId, userId, request))
                .isInstanceOf(UserException.class)
                .satisfies(throwable -> {
                    UserException exception = (UserException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.DUPLICATE_NICKNAME);
                    assertThat(exception.getDetails()).isEqualTo(Map.of("nickname", request.nickname()));
                });
        verify(userRepository).findById(userId);
        verify(userRepository).existsByNickname(request.nickname());
        verify(userMapper, never()).toDto(any());
    }
}
