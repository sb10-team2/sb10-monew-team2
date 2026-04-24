package com.springboot.monew.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.comment.exception.CommentErrorCode;
import com.springboot.monew.comment.mapper.CommentLikeMapper;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.common.exception.MonewException;
import com.springboot.monew.notification.event.CommentLikeNotificationEvent;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CommentLikeServiceTest {

  @Mock private CommentLikeRepository commentLikeRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentLikeMapper commentLikeMapper;
  @Mock private UserRepository userRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private CommentLikeService commentLikeService;

  @Test
  @DisplayName("좋아요 성공 테스트 케이스")
  void like_ReturnsCommentLikeDto_WhenValidRequest() {
    // given, instancio -> random 값으로 객체를 생성해주는 라이브러리 ?
    Comment comment = Instancio.of(Comment.class).create();

    Comment refreshed = Instancio.of(Comment.class)
        .set(field(BaseEntity.class, "id"), comment.getId())
        .set(field(BaseEntity.class, "createdAt"), comment.getCreatedAt())
        .set(field(Comment.class, "article"), comment.getArticle())
        .set(field(Comment.class, "user"), comment.getUser())
        .set(field(Comment.class, "content"), comment.getContent())
        .set(field(Comment.class, "likeCount"), comment.getLikeCount() + 1)
        .create();

    User user = Instancio.of(User.class)
        .set(field(User.class, "deletedAt"), null)
        .create();


    CommentLikeDto expected = new CommentLikeDto(
        UUID.randomUUID(),
        user.getId(),
        Instant.now(),
        refreshed.getId(),
        refreshed.getArticle().getId(),
        refreshed.getUser().getId(),
        refreshed.getUser().getNickname(),
        refreshed.getContent(),
        refreshed.getLikeCount(),
        refreshed.getCreatedAt()
    );

    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId()))
        .willReturn(Optional.of(comment))
            .willReturn(Optional.of(refreshed));
    given(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId()))
        .willReturn(false);
    given(commentLikeMapper.toCommentLikeDto(any(CommentLike.class))).willReturn(expected);

    // when
    CommentLikeDto result = commentLikeService.like(comment.getId(), user.getId());

    // then
    assertThat(result).isEqualTo(expected);

    verify(commentRepository, times(2)).findByIdAndIsDeletedFalse(comment.getId());
    verify(commentLikeRepository).save(any(CommentLike.class));
    verify(commentLikeMapper).toCommentLikeDto(any(CommentLike.class));
    verify(eventPublisher).publishEvent(any(CommentLikeNotificationEvent.class));
  }

  @Test
  @DisplayName("좋아요 실패 테스트 케이스 - SoftDelete 된 User")
  void like_ThrowsException_WhenUserNotFound() {
    // given
    // softDelete된 User 생성
    User user = Instancio.of(User.class)
        .set(field(User.class, "deletedAt"), Instant.now())
        .create();
    Comment comment = Instancio.of(Comment.class).create();

    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId())).willReturn(Optional.of(comment));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    // when & then
    assertThatThrownBy(() -> commentLikeService.like(comment.getId(), user.getId()))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", user.getId()));
        });

    verify(commentRepository, times(1)).findByIdAndIsDeletedFalse(comment.getId());
    verify(userRepository, times(1)).findById(user.getId());
    verify(commentLikeRepository, never()).save(any(CommentLike.class));
    verify(eventPublisher, never()).publishEvent(any(CommentLikeNotificationEvent.class));
    verify(commentLikeMapper, never()).toCommentLikeDto(any(CommentLike.class));
  }

  @Test
  @DisplayName("좋아요 실패 테스트 케이스 - SoftDelete 된 Comment")
  void like_ThrowsException_WhenCommentNotFound() {
    // given
    Comment comment = Instancio.of(Comment.class)
        .set(field(Comment.class, "isDeleted"), true)
        .create();

    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId())).willReturn(Optional.empty());
    // when & then
    assertThatThrownBy(() -> commentLikeService.like(comment.getId(), UUID.randomUUID()))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", comment.getId()));
        });

    verify(commentRepository, times(1)).findByIdAndIsDeletedFalse(comment.getId());
    verify(userRepository, never()).findById(any());
    verify(commentLikeRepository, never()).save(any(CommentLike.class));
    verify(eventPublisher, never()).publishEvent(any(CommentLikeNotificationEvent.class));
    verify(commentLikeMapper, never()).toCommentLikeDto(any(CommentLike.class));
  }

  @Test
  @DisplayName("좋아요 실패 테스트 케이스 - 중복 좋아요")
  void like_ThrowsException_WhenLikeAlreadyExists() {
    // given
    Comment comment = Instancio.of(Comment.class)
          .create();
    User user = Instancio.of(User.class)
        .set(field(User.class, "deletedAt"), null)
        .create();

    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId())).willReturn(Optional.of(comment));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    // 중복 좋아요
    given(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> commentLikeService.like(comment.getId(), user.getId()))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_LIKE_ALREADY_EXISTS);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", comment.getId(), "userId", user.getId()));
        });

    verify(commentRepository, times(1)).findByIdAndIsDeletedFalse(comment.getId());
    verify(userRepository, times(1)).findById(user.getId());
    verify(commentLikeRepository, times(1)).existsByCommentIdAndUserId(comment.getId(), user.getId());
    verify(commentLikeRepository, never()).save(any(CommentLike.class));
    verify(eventPublisher, never()).publishEvent(any(CommentLikeNotificationEvent.class));
    verify(commentLikeMapper, never()).toCommentLikeDto(any(CommentLike.class));
  }

  @Test
  @DisplayName("좋아요 취소 성공 테스트 케이스")
  void unlike_DeletesLikeAndDecrementsCount_WhenValidRequest() {
    // given
    Comment comment = Instancio.of(Comment.class)
        .set(field(Comment.class, "likeCount"), 1)
        .create();

    User user = Instancio.of(User.class)
        .set(field(User.class, "deletedAt"), null)
        .create();

    CommentLike commentLike = new CommentLike(comment, user);

    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId())).willReturn(Optional.of(comment));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(commentLikeRepository.findCommentLikeByCommentAndUser(comment, user)).willReturn(Optional.of(commentLike));

    // when
    commentLikeService.unlike(comment.getId(), user.getId());

    // then
    verify(commentLikeRepository).delete(commentLike);
    verify(commentRepository).decrementLikeCount(comment.getId());
  }

  @Test
  @DisplayName("좋아요 취소 실패 테스트 - UserNotFound, softDelete된 경우")
  void unlike_ThrowsException_WhenUserNotFound() {
    // given
    Comment comment = Instancio.of(Comment.class).create();
    User user = Instancio.of(User.class)
        .set(field(User.class, "deletedAt"), Instant.now())
        .create();

    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId())).willReturn(Optional.of(comment));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

    // when & then
    assertThatThrownBy(() -> commentLikeService.unlike(comment.getId(), user.getId()))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", user.getId()));
        });

    verify(commentRepository, times(1)).findByIdAndIsDeletedFalse(comment.getId());
    verify(userRepository, times(1)).findById(user.getId());
    verify(commentLikeRepository, never()).delete(any(CommentLike.class));
    verify(commentRepository, never()).decrementLikeCount(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(CommentLikeNotificationEvent.class));
  }

  @Test
  @DisplayName("좋아요 취소 실패 테스트 - CommentNotFound")
  void unlike_ThrowsException_WhenCommentNotFound() {
    // given
    Comment comment = Instancio.of(Comment.class)
        .set(field(Comment.class, "isDeleted"), true)
        .create();
    User user = Instancio.of(User.class).create();

    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId()))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentLikeService.unlike(comment.getId(), user.getId()))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", comment.getId()));
        });

    verify(commentRepository, times(1)).findByIdAndIsDeletedFalse(comment.getId());
    verify(userRepository, never()).findById(any());
    verify(commentLikeRepository, never()).delete(any(CommentLike.class));
    verify(commentRepository, never()).decrementLikeCount(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(CommentLikeNotificationEvent.class));
  }

  @Test
  @DisplayName("좋아요 취소 실패 테스트 - CommentLikeNotFound")
  void unlike_ThrowsException_WhenLikeNotFound() {
    // given
    Comment comment = Instancio.of(Comment.class).create();
    User user = Instancio.of(User.class)
            .set(field(User.class, "deletedAt"), null)
                .create();

    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId()))
        .willReturn(Optional.of(comment));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(commentLikeRepository.findCommentLikeByCommentAndUser(comment, user)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentLikeService.unlike(comment.getId(), user.getId()))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_LIKE_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", comment.getId()));
        });

    verify(commentRepository, times(1)).findByIdAndIsDeletedFalse(comment.getId());
    verify(userRepository, times(1)).findById(user.getId());
    verify(commentLikeRepository, times(1)).findCommentLikeByCommentAndUser(comment, user);
    verify(commentLikeRepository, never()).delete(any(CommentLike.class));
    verify(commentRepository, never()).decrementLikeCount(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(CommentLikeNotificationEvent.class));
  }
}