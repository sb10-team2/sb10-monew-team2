package com.springboot.monew.user.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentItem;
import com.springboot.monew.user.document.UserActivityDocument.CommentLikeItem;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserActivityDocumentTest {

  @Test
  @DisplayName("같은 관심사를 다시 구독하면 중복 없이 맨 앞에 재배치한다")
  void addSubscription_MovesDuplicateToFront() {
    // given
    UUID interestId1 = UUID.randomUUID();
    UUID interestId2 = UUID.randomUUID();

    UserActivityDocument document = new UserActivityDocument(
        UUID.randomUUID(),
        "test@example.com",
        "tester",
        Instant.now()
    );

    SubscriptionItem first = new SubscriptionItem(
        UUID.randomUUID(),
        interestId1,
        "금융",
        List.of("주식"),
        Instant.now()
    );
    SubscriptionItem second = new SubscriptionItem(
        UUID.randomUUID(),
        interestId2,
        "부동산",
        List.of("청약"),
        Instant.now()
    );
    SubscriptionItem duplicated = new SubscriptionItem(
        UUID.randomUUID(),
        interestId1,
        "금융",
        List.of("주식", "채권"),
        Instant.now()
    );

    document.addSubscription(first);
    document.addSubscription(second);

    // when
    document.addSubscription(duplicated);

    // then
    assertThat(document.getSubscriptions()).hasSize(2);
    assertThat(document.getSubscriptions().get(0).interestId()).isEqualTo(interestId1);
    assertThat(document.getSubscriptions().get(0).interestKeywords()).containsExactly("주식", "채권");
    assertThat(document.getSubscriptions().get(1).interestId()).isEqualTo(interestId2);
  }

  @Test
  @DisplayName("댓글 수정 시 기존 댓글을 갱신하고 맨 앞에 배치한다")
  void updateComment_UpdatesAndMovesToFront() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId1 = UUID.randomUUID();
    UUID commentId2 = UUID.randomUUID();

    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentItem first = new CommentItem(
        commentId1,
        UUID.randomUUID(),
        "기사1",
        userId,
        "tester",
        "기존 댓글",
        0L,
        Instant.now()
    );
    CommentItem second = new CommentItem(
        commentId2,
        UUID.randomUUID(),
        "기사2",
        userId,
        "tester",
        "다른 댓글",
        1L,
        Instant.now()
    );
    CommentItem updated = new CommentItem(
        commentId1,
        first.articleId(),
        first.articleTitle(),
        userId,
        "tester",
        "수정된 댓글",
        3L,
        first.createdAt()
    );

    document.addComment(first);
    document.addComment(second);

    // when
    document.updateComment(updated);

    // then
    assertThat(document.getComments()).hasSize(2);
    assertThat(document.getComments().get(0).id()).isEqualTo(commentId1);
    assertThat(document.getComments().get(0).content()).isEqualTo("수정된 댓글");
    assertThat(document.getComments().get(0).likeCount()).isEqualTo(3L);
    assertThat(document.getComments().get(1).id()).isEqualTo(commentId2);
  }

  @Test
  @DisplayName("댓글 좋아요 취소는 commentLike id가 아니라 commentId 기준으로 제거한다")
  void removeCommentLike_RemovesByCommentId() {
    // given
    UUID targetCommentId = UUID.randomUUID();
    UUID otherCommentId = UUID.randomUUID();

    UserActivityDocument document = new UserActivityDocument(
        UUID.randomUUID(),
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentLikeItem target = new CommentLikeItem(
        UUID.randomUUID(),
        Instant.now(),
        targetCommentId,
        UUID.randomUUID(),
        "기사1",
        UUID.randomUUID(),
        "writer1",
        "댓글1",
        3L,
        Instant.now()
    );
    CommentLikeItem other = new CommentLikeItem(
        UUID.randomUUID(),
        Instant.now(),
        otherCommentId,
        UUID.randomUUID(),
        "기사2",
        UUID.randomUUID(),
        "writer2",
        "댓글2",
        2L,
        Instant.now()
    );

    document.addCommentLike(target);
    document.addCommentLike(other);

    // when
    document.removeCommentLike(targetCommentId);

    // then
    assertThat(document.getCommentLikes()).hasSize(1);
    assertThat(document.getCommentLikes().get(0).commentId()).isEqualTo(otherCommentId);
  }

  @Test
  @DisplayName("댓글 좋아요 수 갱신 시 대상 댓글만 변경한다")
  void updateCommentLikeCount_UpdatesOnlyTargetComment() {
    // given
    UUID userId = UUID.randomUUID();
    UUID targetCommentId = UUID.randomUUID();
    UUID otherCommentId = UUID.randomUUID();

    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    CommentItem target = new CommentItem(
        targetCommentId,
        UUID.randomUUID(),
        "기사1",
        userId,
        "tester",
        "댓글1",
        0L,
        Instant.now()
    );
    CommentItem other = new CommentItem(
        otherCommentId,
        UUID.randomUUID(),
        "기사2",
        userId,
        "tester",
        "댓글2",
        4L,
        Instant.now()
    );

    document.addComment(target);
    document.addComment(other);

    // when
    document.updateCommentLikeCount(targetCommentId, 7L);

    // then
    // 좋아요 수를 갱신한 대상 댓글만 7로 변경되었는지 검증한다
    assertThat(document.getComments().stream()
        .filter(comment -> comment.id().equals(targetCommentId))
        .findFirst()
        .orElseThrow()
        .likeCount()).isEqualTo(7L);

    // 갱신 대상이 아닌 다른 댓글의 좋아요 수는 기존 값 4가 유지되는지 검증한다
    assertThat(document.getComments().stream()
        .filter(comment -> comment.id().equals(otherCommentId))
        .findFirst()
        .orElseThrow()
        .likeCount()).isEqualTo(4L);
  }

  @Test
  @DisplayName("관심사 키워드 수정 시 해당 관심사만 갱신한다")
  void updateSubscriptionInterest_UpdatesOnlyTargetInterest() {
    // given
    UUID interestId1 = UUID.randomUUID();
    UUID interestId2 = UUID.randomUUID();

    UserActivityDocument document = new UserActivityDocument(
        UUID.randomUUID(),
        "test@example.com",
        "tester",
        Instant.now()
    );

    SubscriptionItem target = new SubscriptionItem(
        UUID.randomUUID(),
        interestId1,
        "금융",
        List.of("주식"),
        Instant.now()
    );
    SubscriptionItem other = new SubscriptionItem(
        UUID.randomUUID(),
        interestId2,
        "부동산",
        List.of("청약"),
        Instant.now()
    );

    document.addSubscription(target);
    document.addSubscription(other);

    // when
    document.updateSubscriptionInterest(interestId1, List.of("주식", "채권"));

    // then
    assertThat(document.getSubscriptions().stream()
        .filter(subscription -> subscription.interestId().equals(interestId1))
        .findFirst()
        .orElseThrow()
        .interestKeywords()).containsExactly("주식", "채권");

    assertThat(document.getSubscriptions().stream()
        .filter(subscription -> subscription.interestId().equals(interestId2))
        .findFirst()
        .orElseThrow()
        .interestKeywords()).containsExactly("청약");
  }

  @Test
  @DisplayName("기사를 다시 조회하면 중복 없이 맨 앞에 재배치한다")
  void addArticleView_MovesDuplicateToFront() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId1 = UUID.randomUUID();
    UUID articleId2 = UUID.randomUUID();

    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    ArticleViewItem first = new ArticleViewItem(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        articleId1,
        ArticleSource.NAVER,
        "https://a.com",
        "기사1",
        Instant.now(),
        "요약1",
        1L,
        10L
    );
    ArticleViewItem second = new ArticleViewItem(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        articleId2,
        ArticleSource.NAVER,
        "https://b.com",
        "기사2",
        Instant.now(),
        "요약2",
        2L,
        20L
    );
    ArticleViewItem duplicated = new ArticleViewItem(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        articleId1,
        ArticleSource.NAVER,
        "https://a.com",
        "기사1-최신",
        Instant.now(),
        "요약1-최신",
        3L,
        30L
    );

    document.addArticleView(first);
    document.addArticleView(second);

    // when
    document.addArticleView(duplicated);

    // then
    assertThat(document.getArticleViews()).hasSize(2);
    assertThat(document.getArticleViews().get(0).articleId()).isEqualTo(articleId1);
    assertThat(document.getArticleViews().get(0).articleTitle()).isEqualTo("기사1-최신");
    assertThat(document.getArticleViews().get(1).articleId()).isEqualTo(articleId2);
  }

  @Test
  @DisplayName("최근 활동은 최대 10개까지만 유지한다")
  void addComment_KeepsOnlyTenRecentItems() {
    // given
    UUID userId = UUID.randomUUID();
    UserActivityDocument document = new UserActivityDocument(
        userId,
        "test@example.com",
        "tester",
        Instant.now()
    );

    UUID firstCommentId = UUID.randomUUID();
    UUID lastCommentId = UUID.randomUUID();

    // when
    for (int i = 0; i < 11; i++) {
      // 첫 댓글과 마지막 댓글만 고정 id로 저장해 나중에 제거/유지 여부를 검증한다.
      UUID commentId = (i == 0) ? firstCommentId : (i == 10 ? lastCommentId : UUID.randomUUID());
      // 댓글 활동 11개를 순서대로 추가한다.
      document.addComment(new CommentItem(
          commentId,
          UUID.randomUUID(),
          "기사" + i,
          userId,
          "tester",
          "댓글" + i,
          0L,
          Instant.now().plusSeconds(i)
      ));
    }

    // then
    // 최근 활동은 최대 10개까지만 유지되어야 한다.
    assertThat(document.getComments()).hasSize(10);

    // 가장 마지막에 추가한 최신 댓글은 맨 앞에 있어야 한다.
    assertThat(document.getComments().get(0).id()).isEqualTo(lastCommentId);

    // 가장 먼저 들어간 오래된 댓글은 제거되어야 한다.
    assertThat(document.getComments().stream()
        .map(CommentItem::id))
        .doesNotContain(firstCommentId);
  }

}
