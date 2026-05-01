package com.springboot.monew.user.outbox.payload.user;

import com.springboot.monew.user.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredPayload(
    UUID userId,
    String email,
    String nickname,
    Instant createdAt
) {
  public static UserRegisteredPayload of(User user) {
    return new UserRegisteredPayload(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getCreatedAt()
    );
  }


}
