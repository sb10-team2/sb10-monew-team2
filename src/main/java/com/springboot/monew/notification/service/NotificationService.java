package com.springboot.monew.notification.service;

import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.entity.ResourceType;
import com.springboot.monew.notification.mapper.NotificationMapper;
import com.springboot.monew.notification.repository.NotificationRepository;
import com.springboot.monew.users.entity.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  public Notification create(String content, ResourceType resourceType, User user,
      Interest interest, CommentLike commentLike) {
    return null;
  }

  public List<NotificationDto> find(NotificationFindRequest request, UUID userId) {
    return null;
  }
}
