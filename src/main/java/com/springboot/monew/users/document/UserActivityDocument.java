package com.springboot.monew.users.document;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "user_activities")
public class UserActivityDocument {
  @Id
  private UUID userId;

  private String email;

  private String nickname;

  private Instant createdAt;

  private List<SubscriptionItem> subscriptions = new ArrayList<>();

  private List<CommentItem> comments = new ArrayList<>();

  private List<CommentLikeItem> commentLikes = new ArrayList<>();

  private List<ArticleViewItem> articleViews = new ArrayList<>();

  public UserActivityDocument(UUID userId, String email, String nickname, Instant createdAt) {
    this.userId = userId;
    this.email = email;
    this.nickname = nickname;
    this.createdAt = createdAt;
  }

  public record SubscriptionItem(
      UUID id,
      UUID interestId,
      String interestName,
      List<String> interestKeywords,
      Long interestSubscriberCount,
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
      Long likeCount,
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
      Long commentLikeCount,
      Instant commentCreatedAt
  ) {
  }

  public record ArticleViewItem(
      UUID id,
      UUID viewedBy,
      Instant createdAt,
      UUID articleId,
      String source,
      String sourceUrl,
      String articleTitle,
      Instant articlePublishedDate,
      String articleSummary,
      Long articleCommentCount,
      Long articleViewCount
  ) {
  }

  }


