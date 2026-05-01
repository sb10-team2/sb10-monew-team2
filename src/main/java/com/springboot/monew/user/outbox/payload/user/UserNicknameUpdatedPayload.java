package com.springboot.monew.user.outbox.payload.user;

import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.event.user.UserNicknameUpdatedEvent;
import java.util.UUID;

public record UserNicknameUpdatedPayload(
    UUID userId,
    String nickname
) {
  public static UserNicknameUpdatedPayload of(User user) {
    return new UserNicknameUpdatedPayload(
        user.getId(),
        user.getNickname()
    );
  }

  public static UserNicknameUpdatedPayload of(UserNicknameUpdatedEvent event) {
    return new UserNicknameUpdatedPayload(
        event.userId(),
        event.nickname()
    );
  }
}
