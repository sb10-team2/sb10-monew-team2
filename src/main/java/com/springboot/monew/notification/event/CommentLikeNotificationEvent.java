package com.springboot.monew.notification.event;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.user.entity.User;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CommentLikeNotificationEvent {

  private final ResourceType resourceType = ResourceType.COMMENT;
  private final UUID userId;
  private final UUID commentLikeId;

  public static CommentLikeNotificationEvent from(User user, CommentLike commentLike) {
    return new CommentLikeNotificationEvent(user.getId(), commentLike.getId());
  }
}
