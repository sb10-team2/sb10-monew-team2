package com.springboot.monew.user.mapper;

import com.springboot.monew.user.dto.response.UserDto;
import com.springboot.monew.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
