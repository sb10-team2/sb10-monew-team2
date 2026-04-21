package com.springboot.monew.notification.event;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.users.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class InterestNotificationEvent {

  private final ResourceType resourceType = ResourceType.INTEREST;
  private final User user;
  private final Interest interest;
}
