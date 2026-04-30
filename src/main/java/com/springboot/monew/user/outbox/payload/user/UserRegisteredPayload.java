package com.springboot.monew.user.outbox.payload.user;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredPayload(
    UUID userId,
    String email,
    String nickname,
    Instant createdAt
) {

}
