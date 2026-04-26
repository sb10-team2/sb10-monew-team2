package com.springboot.monew.users.controller;

import com.springboot.monew.users.dto.request.UserLoginRequest;
import com.springboot.monew.users.dto.request.UserRegisterRequest;
import com.springboot.monew.users.dto.request.UserUpdateRequest;
import com.springboot.monew.users.dto.response.UserDto;
import com.springboot.monew.users.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApiDocs {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> register(
      @Valid @RequestBody UserRegisterRequest request
  ) {
    UserDto userDto = userService.register(request);
    return ResponseEntity.created(URI.create("/api/users/" + userDto.id())).body(userDto);
  }

  @PostMapping("/login")
  public ResponseEntity<UserDto> login(
      @Valid @RequestBody UserLoginRequest request
  ) {
    UserDto userDto = userService.login(request);
    return ResponseEntity.ok(userDto);
  }

  @PatchMapping("/{userId}")
  public ResponseEntity<UserDto> update(
      @PathVariable UUID userId,
      @Valid @RequestBody UserUpdateRequest request
  ) {
        /* 헤더 요청을 보내고 싶어도 프론트엔드에서 Monew-Request-User-ID 헤더를 전달하지 않아,
           그냥 swagger문서 요구사항에 맞게 userId와 요청 본문만으로 닉네임 수정 요청을 처리하도록 변경하였다.
         */
    UserDto userDto = userService.update(userId, request);
    return ResponseEntity.ok(userDto);
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID userId
  ) {
    userService.delete(userId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{userId}/hard")
  public ResponseEntity<Void> hardDelete(
      @PathVariable UUID userId
  ) {
    userService.hardDelete(userId);
    return ResponseEntity.noContent().build();
  }
}
