package com.springboot.monew.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.springboot.monew.users.document.UserActivityDocument;
import com.springboot.monew.users.dto.response.UserActivityDto;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.mapper.UserActivityMapper;
import com.springboot.monew.users.repository.UserActivityRepository;
import com.springboot.monew.users.repository.UserRepository;
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
public class UserActivityServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserActivityRepository userActivityRepository;

  @Mock
  private UserActivityMapper userActivityMapper;

  @InjectMocks
  private UserActivityService userActivityService;

  @Test
  @DisplayName("활동 내역 문서가 있으면 사용자 활동 내역을 조회한다")
  void findUserActivity_success_withDocument() {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-18T00:00:00Z");

    User user = new User("test@example.com", "monew123", "password123");

    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "monew123",
        createdAt
    );

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

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));
    given(userActivityMapper.toDto(document)).willReturn(expected);

    // when
    UserActivityDto result = userActivityService.findUserActivity(userId);

    // then
    assertThat(result).isEqualTo(expected);

    verify(userRepository).findById(userId);
    verify(userActivityRepository).findById(userId);
    verify(userActivityMapper).toDto(document);
    verify(userActivityMapper, never()).toEmptyDto(any());
  }

  @Test
  @DisplayName("활동 내역 문서가 없으면 빈 활동 내역을 반환한다")
  void findUserActivity_success_withEmptyDocument() {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-18T00:00:00Z");

    User user = new User("test@example.com", "monew123", "password123");

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

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());
    given(userActivityMapper.toEmptyDto(user)).willReturn(expected);

    // when
    UserActivityDto result = userActivityService.findUserActivity(userId);

    // then
    assertThat(result).isEqualTo(expected);
    assertThat(result.subscriptions()).isEmpty();
    assertThat(result.comments()).isEmpty();
    assertThat(result.commentLikes()).isEmpty();
    assertThat(result.articleViews()).isEmpty();

    verify(userRepository).findById(userId);
    verify(userActivityRepository).findById(userId);
    verify(userActivityMapper).toEmptyDto(user);
    verify(userActivityMapper, never()).toDto(any(UserActivityDocument.class));
  }

  @Test
  @DisplayName("사용자가 존재하지 않으면 USER_NOT_FOUND 예외가 발생한다")
  void findUserActivity_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userActivityService.findUserActivity(userId))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(userRepository).findById(userId);
    verify(userActivityRepository, never()).findById(any());
  }

  @Test
  @DisplayName("논리 삭제된 사용자 조회 시 USER_NOT_FOUND 예외가 발생한다")
  void findUserActivity_deletedUser() {
    // given
    UUID userId = UUID.randomUUID();

    User deletedUser = new User("deleted@example.com", "monew123", "password123");
    deletedUser.delete();

    given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

    // when & then
    assertThatThrownBy(() -> userActivityService.findUserActivity(userId))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(userRepository).findById(userId);
    verify(userActivityRepository, never()).findById(any());
  }
}
