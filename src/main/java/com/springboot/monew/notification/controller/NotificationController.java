package com.springboot.monew.notification.controller;

import com.springboot.monew.common.dto.CursorPageResponse;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import com.springboot.monew.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 관리", description = "알림 관련 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApiDocs {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorPageResponse<NotificationDto>> find(
      @ModelAttribute @Valid NotificationFindRequest request,
      @RequestHeader("Monew-Request-User-ID") @NotNull UUID userId) {
    CursorPageResponse<NotificationDto> result = notificationService.find(request, userId);
    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  @PatchMapping
  public ResponseEntity<?> bulkUpdate(
      @RequestHeader("Monew-Request-User-ID") @NotNull UUID userId) {
    notificationService.update(userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PatchMapping("/{notificationId}")
  public ResponseEntity<?> update(@PathVariable @NotNull UUID notificationId,
      @RequestHeader("Monew-Request-User-ID") @NotNull UUID userId) {
    notificationService.update(notificationId, userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
