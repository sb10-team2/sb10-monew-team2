package com.springboot.monew.users.document;


import com.springboot.monew.newsarticles.enums.ArticleSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/*
  사용자 활동 내역 조회를 위한 MongoDB 저장 모델이다.

  사용자별 활동 데이터를 한 문서에 모아 저장하여 활동 내역 조회 시 RDBMS의 여러 테이블을 조합하지 않고
  MongoDB 문서 하나로 응답 DTO를 구성할 수 있도록 한다.

  이 모델은 API 응답 DTO와 유사한 구조를 가지지만, 응답 DTO를 직접 저장 타입으로
  사용하지 않고 Document 전용 Item record를 사용해 API 응답 모델과 MongoDB 저장 모델의 책임을 분리한다.

  아래 Item record들은 UserActivityDocument 내부 리스트에 저장되는 한 건의 데이터이다.
  API 응답 DTO와 필드 구조는 유사하지만 MongoDB 저장 전용 타입으로 사용한다.
 */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "user_activities")
public class UserActivityDocument {

  // 최근 활동 목록은 최대 10개까지만 유지한다.
  private static final int RECENT_ACTIVITY_LIMIT = 10;

  @Id
  private UUID id;

  private String email;

  private String nickname;

  private Instant createdAt;

  private List<SubscriptionItem> subscriptions = new ArrayList<>();

  private List<CommentItem> comments = new ArrayList<>();

  private List<CommentLikeItem> commentLikes = new ArrayList<>();

  private List<ArticleViewItem> articleViews = new ArrayList<>();

  public UserActivityDocument(UUID id, String email, String nickname, Instant createdAt) {
    this.id = id;
    this.email = email;
    this.nickname = nickname;
    this.createdAt = createdAt;
  }

  // 사용자 닉네임 변경 시 활동 문서의 닉네임을 갱신한다.
  public void updateNickname(String nickname) {
    this.nickname = nickname;
  }

  // 관심사 구독 성공 시 subscriptions에 추가한다.
  public void addSubscription(SubscriptionItem item) {
    addRecentItem(
        subscriptions,
        item,
        subscription -> subscription.interestId().equals(item.interestId())
    );
  }

  // 관심사 구독 취소 시 subscriptions에서 제거한다.
  public void removeSubscription(UUID interestId) {
    subscriptions.removeIf(subscription -> subscription.interestId().equals(interestId));
  }

  // 댓글 작성 성공 시 comments에 추가한다.
  public void addComment(CommentItem item) {
    addRecentItem(
        comments,
        item,
        comment -> comment.id().equals(item.id())
    );
  }

  // 댓글 수정 성공 시 comments의 해당 댓글 정보를 갱신한다.
  public void updateComment(CommentItem item) {
    comments.removeIf(comment -> comment.id().equals(item.id()));
    comments.add(0, item);
    trimToLimit(comments);
  }

  // 댓글 삭제 성공 시 comments에서 제거한다.
  public void removeComment(UUID commentId) {
    comments.removeIf(comment -> comment.id().equals(commentId));
  }

  // 댓글 좋아요 성공 시 commentLikes에 추가한다.
  public void addCommentLike(CommentLikeItem item) {
    addRecentItem(
        commentLikes,
        item,
        commentLike -> commentLike.id().equals(item.id())
    );
  }

  // 댓글 좋아요 취소 시 commentLikes에서 제거한다.
  public void removeCommentLike(UUID commentId) {
    commentLikes.removeIf(commentLike ->
        commentLike.commentId().equals(commentId));
  }

  // 기사 조회 성공 시 articleViews에 추가한다.
  public void addArticleView(ArticleViewItem item) {
    addRecentItem(
        articleViews,
        item,
        articleView -> articleView.articleId().equals(item.articleId())
    );
  }

  // 내 댓글이 좋아요/좋아요 취소를 받았을 때 comments 안의 likeCount를 갱신한다.
  public void updateCommentLikeCount(UUID commentId, long likeCount) {
    comments.replaceAll(comment -> {
      if (!comment.id().equals(commentId)) {
        return comment;
      }

      return new CommentItem(
          comment.id(),
          comment.articleId(),
          comment.articleTitle(),
          comment.userId(),
          comment.userNickname(),
          comment.content(),
          likeCount,
          comment.createdAt()
      );
    });
  }

  // 중복 활동을 제거한 뒤 최신 활동을 맨 앞에 추가하고 최대 개수를 유지한다.
  // 제네릭을 받아서 true/false를 반환하는 Predicate 함수를 사용
  private <T> void addRecentItem(List<T> items, T newItem, Predicate<T> duplicatedCondition) {
    items.removeIf(duplicatedCondition);
    items.add(0, newItem);
    trimToLimit(items);
  }

  // 최근 활동 목록이 최대 개수를 초과하면 오래된 항목을 제거한다.
  private <T> void trimToLimit(List<T> items) {
    if (items.size() <= RECENT_ACTIVITY_LIMIT) {
      return;
    }

    // 최근 활동 내역 10건 이후로는 다 제거
    items.subList(RECENT_ACTIVITY_LIMIT, items.size()).clear();
  }

  public record SubscriptionItem(
      UUID id,
      UUID interestId,
      String interestName,
      List<String> interestKeywords,
      Instant createdAt
  ) {

  }

  public record CommentItem(
      UUID id,
      UUID articleId,
      String articleTitle,
      UUID userId,
      String userNickname,
      String content,
      long likeCount,
      Instant createdAt
  ) {

  }

  public record CommentLikeItem(
      UUID id,
      Instant createdAt,
      UUID commentId,
      UUID articleId,
      String articleTitle,
      UUID commentUserId,
      String commentUserNickname,
      String commentContent,
      long commentLikeCount,
      Instant commentCreatedAt
  ) {

  }

  public record ArticleViewItem(
      UUID id,
      UUID viewedBy,
      Instant createdAt,
      UUID articleId,
      ArticleSource source,
      String sourceUrl,
      String articleTitle,
      Instant articlePublishedDate,
      String articleSummary,
      long articleCommentCount,
      long articleViewCount
  ) {

  }

  // 관심사 정보가 변경되면 문서의 subscriptions에 저장된 해당 관심사의 키워드를 최신 값으로 갱신한다.
  public void updateSubscriptionInterest(UUID interestId, List<String> keywords) {
    subscriptions.replaceAll(subscription -> {
      if (!subscription.interestId().equals(interestId)) {
        return subscription;
      }

      return new SubscriptionItem(
          subscription.id(),
          subscription.interestId(),
          subscription.interestName(),
          keywords,
          subscription.createdAt()
      );
    });
  }
}
