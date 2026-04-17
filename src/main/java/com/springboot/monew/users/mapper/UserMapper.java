package com.springboot.monew.users.mapper;

import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
