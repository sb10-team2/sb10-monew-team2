package com.springboot.monew.users.controller;

import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.dto.UserLoginRequest;
import com.springboot.monew.users.dto.UserRegisterRequest;
import com.springboot.monew.users.dto.UserUpdateRequest;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody UserUpdateRequest request
  ) {
        /* userId: 수정 대상 사용자 ID
           requestUserId: 로그인한 사용자 ID(요구사항상 헤더로 전달)
           - 아마 닉네임 수정 요청을 보낸 로그인 사용자가 누구인지 판별하기 위해서 헤더로 넣는 것 같음.
           request: 수정할 닉네임 값
           서비스에서 userId와 requestUserId를 비교해 본인 요청인지 검증한 뒤 닉네임을 수정한다.
         */
    UserDto userDto = userService.update(userId, requestUserId, request);
    return ResponseEntity.ok(userDto);
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId
  ) {
    userService.delete(userId, requestUserId);
    return ResponseEntity.noContent().build();
  }
}
