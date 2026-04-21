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


