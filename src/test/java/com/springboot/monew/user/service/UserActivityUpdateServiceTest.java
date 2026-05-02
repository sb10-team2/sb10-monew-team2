package com.springboot.monew.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.outbox.payload.comment.CommentDeletedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentLikeCountUpdatedPayload;
import com.springboot.monew.user.outbox.payload.commentlike.CommentUnlikedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUnsubscribedPayload;
import com.springboot.monew.user.outbox.payload.interest.InterestUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserNicknameUpdatedPayload;
import com.springboot.monew.user.outbox.payload.user.UserRegisteredPayload;
import com.springboot.monew.user.repository.UserActivityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserActivityUpdateServiceTest {

  @Mock
  private UserActivityRepository userActivityRepository;

  @InjectMocks
  private UserActivityUpdateService userActivityUpdateService;

  @Test
  @DisplayName("회원가입 시 사용자 활동 문서를 생성한다")
  void createdUserActivity_SavesNewDocument() {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-30T00:00:00Z");

    UserRegisteredPayload payload = new UserRegisteredPayload(
        userId,
        "test@example.com",
        "tester",
        createdAt
    );

    // when
    userActivityUpdateService.createUserActivity(payload);

    // then
    // 저장된 UserActivityDocument를 가져와 회원가입한 사용자의 기본 정보로 생성되었는지 검증한다.
    ArgumentCaptor<UserActivityDocument> captor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    verify(userActivityRepository).save(captor.capture());

    assertThat(captor.getValue().getId()).isEqualTo(payload.userId());
    assertThat(captor.getValue().getEmail()).isEqualTo(payload.email());
    assertThat(captor.getValue().getNickname()).isEqualTo(payload.nickname());
    assertThat(captor.getValue().getCreatedAt()).isEqualTo(payload.createdAt());
  }

  @Test
  @DisplayName("닉네임 수정 시 사용자 활동 문서의 닉네임을 갱신한다")
  void updateUserNickname_UpdatesNicknameAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "oldNickname",
        Instant.now()
    );
    UserNicknameUpdatedPayload payload =
        new UserNicknameUpdatedPayload(userId, "newNickname");

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.updateUserNickname(payload);

    // then
    // 활동 문서의 닉네임이 새 값으로 실제 변경되었는지 검증한다.
    assertThat(document.getNickname()).isEqualTo("newNickname");
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("관심사 수정 시 해당 관심사를 구독 중인 활동 문서들의 키워드를 갱신한다")
  void updateSubscriptionInterest_UpdatesKeywordsAndSavesAllDocuments() {
    // given
    UUID interestId = UUID.randomUUID();
    UUID anotherInterestId = UUID.randomUUID();

    InterestUpdatedPayload payload =
        new InterestUpdatedPayload(interestId, List.of("주식", "채권"));

    UserActivityDocument document1 = new UserActivityDocument(
        UUID.randomUUID(),
        "user1@example.com",
        "tester1",
        Instant.now()
    );
    UserActivityDocument document2 = new UserActivityDocument(
        UUID.randomUUID(),
        "user2@example.com",
        "tester2",
        Instant.now()
    );

    // 두 활동 문서 모두 수정 대상 관심사를 구독하고 있는 상황을 준비한다.
    SubscriptionItem targetItem1 = new SubscriptionItem(
        UUID.randomUUID(),
        interestId,
        "금융",
        List.of("주식"),
        Instant.now()
    );
    SubscriptionItem targetItem2 = new SubscriptionItem(
        UUID.randomUUID(),
        interestId,
        "금융",
        List.of("주식"),
        Instant.now()
    );

    // 수정 대상이 아닌 다른 관심사 구독 내역은 변경되지 않아야 하므로 함께 추가해 둔다.
    SubscriptionItem otherItem1 = new SubscriptionItem(
        UUID.randomUUID(),
        anotherInterestId,
        "부동산",
        List.of("청약"),
        Instant.now()
    );
    SubscriptionItem otherItem2 = new SubscriptionItem(
        UUID.randomUUID(),
        anotherInterestId,
        "부동산",
        List.of("청약"),
        Instant.now()
    );
    document1.addSubscription(targetItem1);
    document1.addSubscription(otherItem1);

    document2.addSubscription(targetItem2);
    document2.addSubscription(otherItem2);

    given(userActivityRepository.findAllBySubscriptionsInterestId(interestId))
        .willReturn(List.of(document1, document2));

    // when
    userActivityUpdateService.updateSubscriptionInterest(payload);

    // then
    // 두 활동 문서 모두에서 수정 대상 관심사의 키워드가 최신 값으로 갱신되었는지 검증한다.
    assertThat(document1.getSubscriptions().stream()
        .filter(subscription -> subscription.interestId().equals(interestId))
        .findFirst()
        .orElseThrow()
        .interestKeywords()).containsExactly("주식", "채권");

    assertThat(document2.getSubscriptions().stream()
        .filter(subscription -> subscription.interestId().equals(interestId))
        .findFirst()
        .orElseThrow()
        .interestKeywords()).containsExactly("주식", "채권");

    // 수정 대상이 아닌 다른 관심사 구독 내역은 그대로 유지되는지 검증한다.
    assertThat(document1.getSubscriptions().stream()
        .filter(subscription -> subscription.interestId().equals(anotherInterestId))
        .findFirst()
        .orElseThrow()
        .interestKeywords()).containsExactly("청약");

    assertThat(document2.getSubscriptions().stream()
        .filter(subscription -> subscription.interestId().equals(anotherInterestId))
        .findFirst()
        .orElseThrow()
        .interestKeywords()).containsExactly("청약");

    verify(userActivityRepository).saveAll(List.of(document1, document2));
  }

  @Test
  @DisplayName("관심사 구독 시 활동 문서에 구독 내역을 추가한다")
  void addSubscription_AddsItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    SubscriptionItem item = new SubscriptionItem(
        UUID.randomUUID(),
        interestId,
        "금융",
        List.of("주식", "채권"),
        Instant.now()
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.addSubscription(userId, item);

    // then
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("관심사 구독 취소 시 활동 문서에서 구독 내역을 제거한다")
  void removeSubscription_RemovesItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    InterestUnsubscribedPayload payload =
        new InterestUnsubscribedPayload(userId, interestId);

    // 제거 대상 구독 내역을 미리 활동 문서에 추가해 둔다.
    SubscriptionItem item = new SubscriptionItem(
        UUID.randomUUID(),
        interestId,
        "금융",
        List.of("주식", "채권"),
        Instant.now()
    );
    document.addSubscription(item);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.removeSubscription(payload);

    // then
    // 해당 관심사 구독 내역이 활동 문서에서 실제로 제거되었는지 검증한다.
    assertThat(document.getSubscriptions()).isEmpty();
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("댓글 생성 시 활동 문서에 댓글 내역을 추가한다")
  void addComment_AddsItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentItem item = new CommentItem(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0L,
        Instant.now()
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.addComment(userId, item);

    // then
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("댓글 수정 시 활동 문서의 댓글 내역을 갱신한다")
  void updateComment_UpdatesItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentItem item = new CommentItem(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "수정된 댓글",
        1L,
        Instant.now()
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.updateComment(userId, item);

    // then
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("댓글 삭제 시 활동 문서에서 댓글 내역을 제거한다")
  void removeComment_RemovesItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentDeletedPayload payload = CommentDeletedPayload.of(userId, commentId);

    // 제거 대상 댓글 내역을 미리 활동 문서에 추가해 둔다.
    CommentItem item = new CommentItem(
        commentId,
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0L,
        Instant.now()
    );
    document.addComment(item);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.removeComment(payload);

    // then
    // 해당 댓글 내역이 활동 문서에서 실제로 제거되었는지 검증한다.
    assertThat(document.getComments()).isEmpty();
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("댓글 좋아요 시 활동 문서에 좋아요 내역을 추가한다")
  void addCommentLike_AddsItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentLikeItem item = new CommentLikeItem(
        UUID.randomUUID(),
        Instant.now(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        UUID.randomUUID(),
        "commentWriter",
        "댓글 내용",
        3L,
        Instant.now()
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.addCommentLike(userId, item);

    // then
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("댓글 좋아요 취소 시 활동 문서에서 좋아요 내역을 제거한다")
  void removeCommentLike_RemovesItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UUID targetCommentId = UUID.randomUUID();
    UUID otherCommentId = UUID.randomUUID();

    CommentUnlikedPayload payload = CommentUnlikedPayload.of(userId, targetCommentId);

    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    // 좋아요 내역의 id와 commentId는 다른 값이므로, commentId 기준으로 삭제되는지 확인할 수 있게 준비한다.
    CommentLikeItem targetItem = new CommentLikeItem(
        UUID.randomUUID(),
        Instant.now(),
        targetCommentId,
        UUID.randomUUID(),
        "기사 제목1",
        UUID.randomUUID(),
        "commentWriter1",
        "댓글 내용1",
        3L,
        Instant.now()
    );

    // 삭제 대상이 아닌 다른 좋아요 내역도 함께 추가해 두고 유지되는지 확인한다.
    CommentLikeItem otherItem = new CommentLikeItem(
        UUID.randomUUID(),
        Instant.now(),
        otherCommentId,
        UUID.randomUUID(),
        "기사 제목2",
        UUID.randomUUID(),
        "commentWriter2",
        "댓글 내용2",
        2L,
        Instant.now()
    );
    // 삭제 대상 좋아요 내역과 비교 대상 좋아요 내역을 활동 문서에 미리 추가해 둔다.
    document.addCommentLike(targetItem);
    document.addCommentLike(otherItem);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.removeCommentLike(payload);

    // then
    // 좋아요 내역이 commentLike id가 아니라 commentId 기준으로 실제 제거되었는지 검증한다.
    assertThat(document.getCommentLikes()).hasSize(1);
    assertThat(document.getCommentLikes().get(0).commentId()).isEqualTo(otherCommentId);

    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("댓글 좋아요 수 갱신 시 활동 문서의 댓글 좋아요 수를 갱신한다")
  void updateCommentLikeCount_UpdatesLikeCountAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentLikeCountUpdatedPayload payload = CommentLikeCountUpdatedPayload.of(userId, commentId, 5L);

    // 좋아요 수 변경 대상 댓글을 미리 활동 문서에 추가해 둔다.
    CommentItem item = new CommentItem(
        commentId,
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0L,
        Instant.now()
    );
    document.addComment(item);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.updateCommentLikeCount(payload);

    // then
    // 해당 댓글의 좋아요 수가 새 값으로 실제 갱신되었는지 검증한다.
    assertThat(document.getComments()).hasSize(1);
    assertThat(document.getComments().get(0).likeCount()).isEqualTo(5L);
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("기사 조회 시 활동 문서에 기사 조회 내역을 추가한다")
  void addArticleView_AddsItemAndSavesDocument() {
    // given
    UUID userId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    ArticleViewItem item = new ArticleViewItem(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        UUID.randomUUID(),
        null,
        "https://example.com",
        "기사 제목",
        Instant.now(),
        "기사 요약",
        2L,
        10L
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityUpdateService.addArticleView(userId, item);

    // then
    verify(userActivityRepository).save(document);
  }

  @Test
  @DisplayName("활동 문서가 존재하지 않으면 USER_ACTIVITY_NOT_FOUND 예외가 발생한다")
  void getDocument_ThrowsException_WhenDocumentNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    CommentItem item = new CommentItem(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0L,
        Instant.now()
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userActivityUpdateService.addComment(userId, item))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ACTIVITY_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(userActivityRepository).findById(userId);
  }
}
