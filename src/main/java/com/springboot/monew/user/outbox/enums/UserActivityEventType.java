package com.springboot.monew.user.outbox.enums;

// 사용자 활동 Outbox에 저장되는 이벤트 타입을 나타낸다.
public enum UserActivityEventType {
  USER_REGISTERED,
  USER_NICKNAME_UPDATED,
  INTEREST_SUBSCRIBED,
  INTEREST_UNSUBSCRIBED,
  INTEREST_UPDATED,
  COMMENT_CREATED,
  COMMENT_UPDATED,
  COMMENT_DELETED,
  COMMENT_LIKED,
  COMMENT_UNLIKED,
  COMMENT_LIKE_COUNT_UPDATED,
  ARTICLE_VIEWED
}
