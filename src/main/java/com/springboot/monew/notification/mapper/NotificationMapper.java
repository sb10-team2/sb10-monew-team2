package com.springboot.monew.notification.mapper;

import com.springboot.monew.common.mapper.BaseMapper;
import com.springboot.monew.common.mapper.CommonMapperConfig;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.user.entity.User;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface NotificationMapper extends BaseMapper<Notification, NotificationDto> {

  @Override
  @Mapping(target = "userId", source = "user.id")
  NotificationDto toDto(Notification entity);

  default List<Notification> toEntities(List<User> users, Interest interest,
      ResourceType resourceType) {
    if (users == null || users.isEmpty()) {
      return Collections.emptyList();
    }
    return users.stream()
        .map(user -> toEntityFrom(user, interest, resourceType))
        .toList();
  }

  Notification toEntityFrom(User user, Interest interest, ResourceType resourceType);
}
