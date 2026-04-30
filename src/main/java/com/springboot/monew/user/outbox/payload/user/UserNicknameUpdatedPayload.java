package com.springboot.monew.user.outbox.payload.user;

import java.util.UUID;

public record UserNicknameUpdatedPayload(
    UUID userId,
    String nickname
) {

}
