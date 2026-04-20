package com.springboot.monew.notification.mapper;

import com.springboot.monew.common.mapper.BaseMapper;
import com.springboot.monew.common.mapper.CommonMapperConfig;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CommonMapperConfig.class)
public interface NotificationMapper extends BaseMapper<Notification, NotificationDto> {

  @Override
  @Mapping(target = "userId", source = "user.id")
  NotificationDto toDto(Notification entity);
}
