package com.springboot.monew.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Size(min = 5, max = 100, message = "이메일은 5자 이상 100자 이하여야 합니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Pattern(regexp = "^\\S+$", message = "이메일에 공백이 포함될 수 없습니다.")
        String email,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname
) {
}
