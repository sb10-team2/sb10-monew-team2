package com.springboot.monew.user.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.springboot.monew.user.dto.request.UserLoginRequest;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.request.UserUpdateRequest;
import com.springboot.monew.user.dto.response.UserDto;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.mapper.UserMapper;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @Mock
  private UserActivityOutboxService userActivityOutboxService;

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

    given(userRepository.existsByEmail(request.email())).willReturn(true);

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

    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(userRepository.existsByNickname(request.nickname())).willReturn(true);

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

    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(userRepository.existsByNickname(request.nickname())).willReturn(false);
    given(userRepository.save(any(User.class))).willReturn(savedUser);
    given(userMapper.toDto(savedUser)).willReturn(expected);

    // when
    UserDto result = userService.register(request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(userRepository).existsByEmail(request.email());
    verify(userRepository).existsByNickname(request.nickname());
    verify(userRepository).save(any(User.class));
    verify(userMapper).toDto(savedUser);
    verify(userActivityOutboxService).saveUserRegistered(savedUser);
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

    given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(expected);

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

    given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

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

    given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

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

    given(userRepository.findByEmail("deleted@example.com")).willReturn(Optional.of(deletedUser));

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
    User user = new User("test@example.com", "oldNickname", "password123");

    UserDto expected = new UserDto(
        userId,
        "test@example.com",
        "newNickname",
        Instant.parse("2026-04-18T00:00:00Z")
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByNickname(request.nickname())).willReturn(false);
    given(userMapper.toDto(user)).willReturn(expected);

    // when
    UserDto result = userService.update(userId, request);

    // then
    assertThat(result).isEqualTo(expected);
    assertThat(user.getNickname()).isEqualTo(request.nickname());
    verify(userRepository).findById(userId);
    verify(userRepository).existsByNickname(request.nickname());
    verify(userMapper).toDto(user);
    verify(userActivityOutboxService).saveUserNicknameUpdated(user);
  }

  @Test
  @DisplayName("존재하지 않는 사용자의 닉네임을 수정하면 USER_NOT_FOUND 예외가 발생한다")
  void update_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newNickname");

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request))
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
  @DisplayName("삭제된 사용자의 닉네임을 수정하면 USER_NOT_FOUND 예외가 발생한다.")
  void update_deletedUser() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newNickname");
    User deletedUser = new User("deleted@example.com", "monew123", "password123");
    deletedUser.delete();

    given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(userRepository).findById(userId);
    verify(userRepository, never()).existsByNickname(anyString());
  }

  @Test
  @DisplayName("이미 사용 중인 닉네임으로 수정하면 DUPLICATE_NICKNAME 예외가 발생한다")
  void update_duplicateNickname() {
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("duplicateNickname");
    User user = new User("test@example.com", "oldNickname", "password123");

    // given
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.existsByNickname(request.nickname())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.update(userId, request))
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

  @Test
  @DisplayName("기존 닉네임으로 수정 요청하면 중복 닉네임 조회 없이 성공한다")
  void update_sameNickname() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("sameNickname");
    User user = new User("test@example.com", "sameNickname", "password123");

    UserDto expected = new UserDto(
        userId,
        "test@example.com",
        "sameNickname",
        Instant.parse("2026-04-18T00:00:00Z")
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(expected);

    // when
    UserDto result = userService.update(userId, request);

    // then
    assertThat(result).isEqualTo(expected);
    assertThat(user.getNickname()).isEqualTo(request.nickname());
    verify(userRepository).findById(userId);
    verify(userRepository, never()).existsByNickname(anyString());
    verify(userMapper).toDto(user);
    verify(userActivityOutboxService).saveUserNicknameUpdated(user);
  }

  @Test
  @DisplayName("사용자 삭제에 성공하면 deletedAt이 설정된다")
  void delete_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "monew123", "password123");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    // 삭제 되기 전 확인 검증용
    assertThat(user.isDeleted()).isFalse();

    // when
    userService.delete(userId);

    // then
    assertThat(user.isDeleted()).isTrue();
    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("존재하지 않는 사용자를 삭제하면 USER_NOT_FOUND 예외가 발생한다")
  void delete_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.delete(userId))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });
    verify(userRepository).findById(userId);
  }

  @Test
  @DisplayName("이미 삭제된 사용자를 삭제하면 USER_NOT_FOUND 예외가 발생한다")
  void delete_deletedUser() {
    // given
    UUID userId = UUID.randomUUID();
    User deletedUser = new User("deleted@example.com", "monew123", "password123");
    deletedUser.delete();

    given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

    // when & then
    assertThatThrownBy(() -> userService.delete(userId))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });
    verify(userRepository).findById(userId);
    assertThat(deletedUser.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("사용자 물리 삭제에 성공하면 사용자 엔티티를 삭제한다.")
  void hardDelete_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("test@example.com", "monew123", "password123");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when
    userService.hardDelete(userId);

    // then
    verify(userRepository).findById(userId);
    verify(userRepository).delete(user);
  }

  @Test
  @DisplayName("존재하지 않는 사용자를 물리 삭제하면 USER_NOT_FOUND 예외가 발생한다")
  void hardDelete_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.hardDelete(userId))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(userRepository).findById(userId);
    verify(userRepository, never()).delete(any());
  }

  @Test
  @DisplayName("기준 시각 이전에 논리 삭제된 사용자를 모두 물리 삭제한다")
  void purgeDeletedUsersOlderThan_success() {
    // given
    Instant cutoff = Instant.parse("2026-04-20T00:00:00Z");

    User user1 = new User("deleted1@example.com", "monew123", "password123");
    User user2 = new User("deleted2@example.com", "monew123", "password123");
    List<User> users = List.of(user1, user2);

    given(userRepository.findUsersDeletedBefore(cutoff)).willReturn(users);

    // when
    int result = userService.purgeDeletedUsersOlderThan(cutoff);

    // then
    verify(userRepository).findUsersDeletedBefore(cutoff);
    verify(userRepository).deleteAll(users);
    assertThat(result).isEqualTo(2);
  }

  @Test
  @DisplayName("물리 삭제 대상 사용자가 없으면 삭제를 수행하지 않고 0을 반환한다")
  void purgeDeletedUsersOlderThan_empty() {
    // given
    Instant cutoff = Instant.parse("2026-04-20T00:00:00Z");

    given(userRepository.findUsersDeletedBefore(cutoff)).willReturn(List.of());

    // when
    int result = userService.purgeDeletedUsersOlderThan(cutoff);

    // then
    verify(userRepository).findUsersDeletedBefore(cutoff);
    verify(userRepository, never()).deleteAll(anyList());
    assertThat(result).isEqualTo(0);
  }
}
