package com.springboot.monew.notification.service;

import com.springboot.monew.common.dto.CursorPageResponse;
import com.springboot.monew.common.utils.TimeConverter;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import com.springboot.monew.notification.entity.Notification;
import com.springboot.monew.notification.event.CommentLikeNotificationEvent;
import com.springboot.monew.notification.event.InterestNotificationEvent;
import com.springboot.monew.notification.exception.NotificationErrorCode;
import com.springboot.monew.notification.exception.NotificationException;
import com.springboot.monew.notification.mapper.NotificationMapper;
import com.springboot.monew.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  public NotificationDto create(InterestNotificationEvent event) {
    Notification notification = notificationMapper.toEntityFrom(event);
    notificationRepository.save(notification);
    return notificationMapper.toDto(notification);
  }

  public NotificationDto create(CommentLikeNotificationEvent event) {
    Notification notification = notificationMapper.toEntityFrom(event);
    notificationRepository.save(notification);
    return notificationMapper.toDto(notification);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<NotificationDto> find(NotificationFindRequest request, UUID userId) {
    Pageable pageable = PageRequest.of(0, request.getLimit());
    Slice<Notification> results = notificationRepository.findByCursor(request.getCursor(),
        request.getAfter(), userId, pageable);
    return toCursorPage(results, userId);
  }

  public void update(UUID id, UUID userId) {
    Notification notification = notificationRepository.findById(id).orElseThrow(
        () -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND, id)
    );
    notification.updateConfirmed(Instant.now());
  }

  public void update(UUID userId) {
    notificationRepository.bulkUpdateConfirmed(userId, Instant.now());
  }

  private CursorPageResponse<NotificationDto> toCursorPage(Slice<Notification> results,
      UUID userId) {
    List<Notification> entities = results.getContent();
    boolean hasNext = results.hasNext();
    String nextCursor = null;
    String nextAfter = null;
    int size = entities.size();
    long totalElements = notificationRepository.countAllByUser_IdAndConfirmedIsFalse(userId);
    if (hasNext && !entities.isEmpty()) {
      Notification lastNotification = entities.get(size - 1);
      nextCursor = lastNotification.getId().toString();
      nextAfter = TimeConverter.toDatetime(lastNotification.getCreatedAt()).toString();
    }
    return new CursorPageResponse<>(
        notificationMapper.toDto(entities),
        nextCursor,
        nextAfter,
        size,
        totalElements,
        hasNext
    );
  }
}
