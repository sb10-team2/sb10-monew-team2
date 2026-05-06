package com.springboot.monew.user.event.user;

import java.util.UUID;

public record UserNicknameUpdatedEvent(
    UUID userId,
    String nickname
) {

}
