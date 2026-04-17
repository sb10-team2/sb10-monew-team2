package com.springboot.monew.notification.entity;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.exception.NotificationErrorCode;
import com.springboot.monew.notification.exception.NotificationException;
import com.springboot.monew.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    public Notification(String content,
                        ResourceType resourceType,
                        User user,
                        Interest interest,
                        CommentLike commentLike) {
        this.content = content;
        this.resourceType = resourceType;
        this.user = user;
        this.interest = interest;
        this.commentLike = commentLike;
    }

    @Transient
    public UUID getResourceId() {
        if (resourceType == ResourceType.COMMENT && commentLike != null) {
            return commentLike.getId();
        }
        if (resourceType == ResourceType.INTEREST && interest != null) {
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

    public Optional<Interest> getInterest() {
        return Optional.ofNullable(interest);
    }

    public Optional<CommentLike> getCommentLike() {
        return Optional.ofNullable(commentLike);
    }

    public void updateConfirmed() {
        if (!confirmed) {
            confirmed = true;
            updatedAt = Instant.now();
        }
    }
}
