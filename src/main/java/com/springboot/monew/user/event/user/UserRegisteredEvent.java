package com.springboot.monew.user.event.user;

import com.springboot.monew.user.entity.User;

public record UserRegisteredEvent(
    User user
) {

}
