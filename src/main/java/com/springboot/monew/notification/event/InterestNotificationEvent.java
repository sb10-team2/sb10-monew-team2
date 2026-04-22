package com.springboot.monew.notification.event;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.notification.entity.ResourceType;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class InterestNotificationEvent {

  private final ResourceType resourceType = ResourceType.INTEREST;
  private final UUID interestId;

  public static List<InterestNotificationEvent> from(List<ArticleInterest> articleInterests) {
    return articleInterests.stream()
        .map(ArticleInterest::getInterest)
        .map(Interest::getId)
        .map(InterestNotificationEvent::new)
        .toList();
  }
}
