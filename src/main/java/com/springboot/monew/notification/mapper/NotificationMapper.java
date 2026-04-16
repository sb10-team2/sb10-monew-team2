package com.springboot.monew.notification.mapper;

import com.springboot.monew.common.mapper.BaseMapper;
import com.springboot.monew.common.mapper.CommonMapperConfig;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.entity.Notification;
import org.mapstruct.*;

import java.time.Instant;

@Mapper(config = CommonMapperConfig.class)
public interface NotificationMapper extends BaseMapper<Notification, NotificationDto> {
    @Override
    @Mapping(target = "userId", source = "user.id")
    NotificationDto toDto(Notification entity);

    default Notification partialUpdate(Notification notification) {
        notification.setUpdatedAt(Instant.now());
        notification.setConfirmed(true);
        return notification;
    }
}
