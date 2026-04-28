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
import com.springboot.monew.users.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.event.comment.CommentLikeCountUpdatedEvent;
import com.springboot.monew.users.event.comment.CommentLikedEvent;
import com.springboot.monew.users.event.comment.CommentUnlikedEvent;
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
import org.mockito.ArgumentCaptor;
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

    // 사용자 활동 이벤트 발행 시 CommentLikedEvent 안에 담길 CommentLikeItem을 미리 준비한다.
    CommentLikeItem commentLikeItem = new CommentLikeItem(
        UUID.randomUUID(),
        Instant.now(),
        refreshed.getId(),
        refreshed.getArticle().getId(),
        refreshed.getArticle().getTitle(),
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
    // like() 내부에서 CommentLikedEvent 생성 시 toCommentLikeItem()을 호출하므로 null이 반환되지 않도록 stub 처리한다.
    given(commentLikeMapper.toCommentLikeItem(any(CommentLike.class))).willReturn(commentLikeItem);

    // when
    CommentLikeDto result = commentLikeService.like(comment.getId(), user.getId());

    // then
    assertThat(result).isEqualTo(expected);

    verify(commentRepository, times(2)).findByIdAndIsDeletedFalse(comment.getId());
    verify(commentLikeRepository).save(any(CommentLike.class));
    verify(commentLikeMapper).toCommentLikeDto(any(CommentLike.class));

    // 좋아요 등록 시 총 3개의 이벤트가 발행되는지 검증하고, 전달된 이벤트 객체들을 모두 가져온다.
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, times(3)).publishEvent(captor.capture());

    // 발행된 이벤트 목록에서 사용자 활동 내역 추가 이벤트를 찾는다.
    CommentLikedEvent likedEvent = captor.getAllValues().stream()
        .filter(CommentLikedEvent.class::isInstance)
        .map(CommentLikedEvent.class::cast)
        .findFirst()
        .orElseThrow();

    // 발행된 이벤트 목록에서 댓글 좋아요 수 갱신 이벤트를 찾는다.
    CommentLikeCountUpdatedEvent likeCountUpdatedEvent = captor.getAllValues().stream()
        .filter(CommentLikeCountUpdatedEvent.class::isInstance)
        .map(CommentLikeCountUpdatedEvent.class::cast)
        .findFirst()
        .orElseThrow();

    // 발행된 이벤트 목록에서 댓글 좋아요 알림 이벤트를 찾는다.
    CommentLikeNotificationEvent notificationEvent = captor.getAllValues().stream()
        .filter(CommentLikeNotificationEvent.class::isInstance)
        .map(CommentLikeNotificationEvent.class::cast)
        .findFirst()
        .orElseThrow();

    // 사용자 활동 내역에 추가할 좋아요 이벤트가 올바르게 발행되었는지 검증한다.
    assertThat(likedEvent.userId()).isEqualTo(user.getId());
    assertThat(likedEvent.item().commentId()).isEqualTo(refreshed.getId());
    assertThat(likedEvent.item().articleId()).isEqualTo(refreshed.getArticle().getId());
    assertThat(likedEvent.item().commentContent()).isEqualTo(refreshed.getContent());

    // 댓글 작성자의 활동 내역에 반영할 좋아요 수 갱신 이벤트가 올바르게 발행되었는지 검증한다.
    assertThat(likeCountUpdatedEvent.userId()).isEqualTo(refreshed.getUser().getId());
    assertThat(likeCountUpdatedEvent.commentId()).isEqualTo(refreshed.getId());
    assertThat(likeCountUpdatedEvent.likeCount()).isEqualTo(refreshed.getLikeCount());

    // 댓글 좋아요 알림 이벤트가 함께 발행되었는지 검증한다.
    assertThat(notificationEvent).isNotNull();
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

    // 좋아요 수가 감소한 뒤 다시 조회되는 댓글 객체를 미리 준비한다.
    Comment refreshed = Instancio.of(Comment.class)
        .set(field(BaseEntity.class, "id"), comment.getId())
        .set(field(BaseEntity.class, "createdAt"), comment.getCreatedAt())
        .set(field(Comment.class, "article"), comment.getArticle())
        .set(field(Comment.class, "user"), comment.getUser())
        .set(field(Comment.class, "content"), comment.getContent())
        .set(field(Comment.class, "likeCount"), comment.getLikeCount() - 1)
        .create();

    // unlike() 내부에서 좋아요 수 감소 후 댓글을 다시 조회하므로, 감소된 likeCount가 반영된 댓글을 반환하도록 설정한다.
    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId())).willReturn(Optional.of(comment))
        .willReturn(Optional.of(refreshed));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(commentLikeRepository.findCommentLikeByCommentAndUser(comment, user)).willReturn(Optional.of(commentLike));

    // when
    commentLikeService.unlike(comment.getId(), user.getId());

    // then
    verify(commentLikeRepository).delete(commentLike);
    verify(commentRepository).decrementLikeCount(comment.getId());

    // 좋아요 취소 시 총 2개의 이벤트가 발행되는지 검증하고, 전달된 이벤트 객체들을 모두 가져온다.
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, times(2)).publishEvent(captor.capture());

    // 발행된 이벤트 목록에서 사용자 활동 내역 제거 이벤트를 찾는다.
    CommentUnlikedEvent unlikedEvent = captor.getAllValues().stream()
        .filter(CommentUnlikedEvent.class::isInstance)
        .map(CommentUnlikedEvent.class::cast)
        .findFirst()
        .orElseThrow();

    // 발행된 이벤트 목록에서 댓글 좋아요 수 갱신 이벤트를 찾는다.
    CommentLikeCountUpdatedEvent likeCountUpdatedEvent = captor.getAllValues().stream()
        .filter(CommentLikeCountUpdatedEvent.class::isInstance)
        .map(CommentLikeCountUpdatedEvent.class::cast)
        .findFirst()
        .orElseThrow();

    // 사용자 활동 내역에서 제거할 좋아요 취소 이벤트가 올바르게 발행되었는지 검증한다.
    assertThat(unlikedEvent.userId()).isEqualTo(user.getId());
    assertThat(unlikedEvent.commentId()).isEqualTo(comment.getId());

    // 댓글 작성자의 활동 내역에 반영할 좋아요 수 갱신 이벤트가 올바르게 발행되었는지 검증한다.
    assertThat(likeCountUpdatedEvent.userId()).isEqualTo(refreshed.getUser().getId());
    assertThat(likeCountUpdatedEvent.commentId()).isEqualTo(refreshed.getId());
    assertThat(likeCountUpdatedEvent.likeCount()).isEqualTo(refreshed.getLikeCount());
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