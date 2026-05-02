package com.springboot.monew.user.outbox.enums;

// 사용자 활동 Outbox 이벤트의 집계 대상 유형을 나타낸다.
public enum UserActivityAggregateType {
  USER,
  INTEREST,
  SUBSCRIPTION,
  COMMENT,
  COMMENT_LIKE,
  ARTICLE_VIEW
}
