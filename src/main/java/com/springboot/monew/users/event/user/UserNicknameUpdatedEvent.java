package com.springboot.monew.users.event.user;

import java.util.UUID;

public record UserNicknameUpdatedEvent(
    UUID userId,
    String nickname
) {

}
