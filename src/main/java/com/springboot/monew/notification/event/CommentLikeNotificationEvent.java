package com.springboot.monew.notification.event;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CommentLikeNotificationEvent {

  private final ResourceType resourceType = ResourceType.COMMENT;
  private final User user;
  private final CommentLike commentLike;
}
