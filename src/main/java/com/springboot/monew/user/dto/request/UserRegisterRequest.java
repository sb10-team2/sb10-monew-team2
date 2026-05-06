package com.springboot.monew.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 정보")
public record UserRegisterRequest(
    @Schema(description = "가입 이메일")
    @NotBlank(message = "이메일은 필수입니다.")
    @Size(min = 5, max = 100, message = "이메일은 5자 이상 100자 이하여야 합니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Pattern(regexp = "^\\S+$", message = "이메일에 공백이 포함될 수 없습니다.")
    String email,

    @Schema(description = "가입 닉네임", minLength = 1, maxLength = 20)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
    String nickname,

    @Schema(description = "가입 비밀번호", minLength = 6, maxLength = 20)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^\\S+$", message = "비밀번호에 공백이 포함될 수 없습니다.")
    @Pattern(regexp = ".*[A-Za-z].*", message = "비밀번호에는 영문이 포함되어야 합니다.")
    @Pattern(regexp = ".*\\d.*", message = "비밀번호에는 숫자가 포함되어야 합니다.")
    @Pattern(
        regexp = ".*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/\\\\|`~].*",
        message = "비밀번호에는 특수문자가 포함되어야 합니다."
    )
    String password
) {

}
