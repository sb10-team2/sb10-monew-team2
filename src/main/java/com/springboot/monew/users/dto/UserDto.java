package com.springboot.monew.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "사용자 정보")
public record UserDto(
        @Schema(description = "사용자 ID", format = "uuid")
        UUID id,

        @Schema(description = "이메일")
        String email,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "가입한 날짜", format = "date-time")
        Instant createdAt
) {
}
