package com.springboot.monew.notification.entity;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.exception.NotificationErrorCode;
import com.springboot.monew.notification.exception.NotificationException;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "confirmed", nullable = false)
  private Boolean confirmed = false;

  @Column(name = "content", nullable = false, length = 100)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false, length = 10)
  private ResourceType resourceType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id")
  private Interest interest;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_likes_id")
  private CommentLike commentLike;

  @Builder
  public Notification(ResourceType resourceType,
      User user,
      Interest interest,
      CommentLike commentLike) {
    this.resourceType = resourceType;
    this.user = user;
    this.interest = interest;
    this.commentLike = commentLike;
    validateDomainConstraint();
    initContent();
  }

  @Transient
  public UUID getResourceId() {
    if (isCommentLikeNotification()) {
      return commentLike.getId();
    }
    if (isInterestNotification()) {
      return interest.getId();
    }
    Map<String, Object> details = new HashMap<>();
    UUID commentLikeId = commentLike == null ? null : commentLike.getId();
    UUID interestId = interest == null ? null : interest.getId();
    details.put("resourceType", resourceType);
    details.put("commentLike", commentLikeId);
    details.put("interest", interestId);
    throw new NotificationException(NotificationErrorCode.INVALID_DATA, details);
  }

  public void updateConfirmed(Instant updatedAt) {
    if (confirmed) {
      throw new NotificationException(NotificationErrorCode.ALREADY_CONFIRMED, getId());
    }
    confirmed = true;
    this.updatedAt = updatedAt;
  }

  private void initContent() {
    String commentLikeMessage = "%s님이 나의 댓글을 좋아합니다.";
    String interestMessage = "%s와 관련된 기사가 %s건 등록되었습니다.";
    if (isInterestNotification()) {
      content = interestMessage.formatted(interest.getName(), interest.getArticleCount());
      return;
    }
    content = commentLikeMessage.formatted(commentLike.getUser().getNickname());
  }

  private void validateDomainConstraint() {
    if (user == null) {
      throw new NotificationException(NotificationErrorCode.MISSING_REQUIRED_FIELD,
          "User를 찾을 수 없습니다");
    }
    if (isInterestNotification()) {
      return;
    }
    if (isCommentLikeNotification()) {
      return;
    }
    throw new NotificationException(NotificationErrorCode.DOMAIN_CONSTRAINT_VIOLATED);
  }

  private boolean isInterestNotification() {
    return resourceType == ResourceType.INTEREST && interest != null && commentLike == null;
  }

  private boolean isCommentLikeNotification() {
    return resourceType == ResourceType.COMMENT && commentLike != null && interest == null;
  }
}
