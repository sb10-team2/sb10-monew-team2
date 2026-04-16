package com.springboot.monew.users.controller;

import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.dto.UserRegisterRequest;
import com.springboot.monew.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
}
