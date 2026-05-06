package com.springboot.monew.notification.event.listener;

import com.springboot.monew.common.exception.MonewException;
import com.springboot.monew.notification.event.CommentLikeNotificationEvent;
import com.springboot.monew.notification.event.InterestNotificationEvent;
import com.springboot.monew.notification.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  @Async("notificationCreationPool")
  @TransactionalEventListener
  public void handleInterestNotificationCreation(InterestNotificationEvent event) {
    try {
      notificationService.create(event);
    } catch (MonewException exception) {
      log.error("[관심사 알림 생성 실패] interestId: {}, resourceType: {}, message: {}",
          event.getInterestId(),
          event.getResourceType(),
          exception.getDetails()
      );
    }
  }

  @Async("notificationCreationPool")
  @TransactionalEventListener
  public void handleCommentLikeNotificationCreation(CommentLikeNotificationEvent event) {
    notificationService.create(event);
  }
}
