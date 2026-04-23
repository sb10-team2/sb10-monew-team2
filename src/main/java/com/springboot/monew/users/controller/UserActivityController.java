package com.springboot.monew.users.controller;

import com.springboot.monew.users.dto.response.UserActivityDto;
import com.springboot.monew.users.service.UserActivityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-activities")
@RequiredArgsConstructor
public class UserActivityController implements UserActivityDocs {

  private final UserActivityService userActivityService;

  @GetMapping("/{userId}")
  public ResponseEntity<UserActivityDto> getUserActivity(
      @PathVariable UUID userId
  ) {
    UserActivityDto userAct = userActivityService.findUserActivity(userId);
    return ResponseEntity.ok(userAct);
  }
}
