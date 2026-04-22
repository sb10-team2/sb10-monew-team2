package com.springboot.monew.notification.event.listener;

import com.springboot.monew.notification.event.CommentLikeNotificationEvent;
import com.springboot.monew.notification.event.InterestNotificationEvent;
import com.springboot.monew.notification.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  @Async("notificationCreationExecutor")
  @TransactionalEventListener
  public void handleInterestNotificationCreation(List<InterestNotificationEvent> events) {
    for (InterestNotificationEvent event : events) {
      notificationService.create(event);
    }
  }

  @Async("notificationCreationExecutor")
  @TransactionalEventListener
  public void handleCommentLikeNotificationCreation(CommentLikeNotificationEvent event) {
    notificationService.create(event);
  }
}
