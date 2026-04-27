package com.springboot.monew.users.event.user;

import com.springboot.monew.users.entity.User;

public record UserRegisteredEvent(
    User user
) {

}
