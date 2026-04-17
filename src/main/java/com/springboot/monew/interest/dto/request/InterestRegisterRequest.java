package com.springboot.monew.interest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// 관심사 등록 요청 dto
public record InterestRegisterRequest(
        @NotBlank(message = "관심사 이름은 필수입니다.")
        @Size(min = 1, max = 50, message = "관심사 이름은 1자 이상 50자 이하로 입력해주세요.")
        String name,

        @NotEmpty(message = "키워드는 최소 1개 이상 입력해야 합니다.")
        @Size(min = 1, max = 10, message = "키워드는 최대 10개까지 입력할 수 있습니다.")
        List<
                @NotBlank(message = "키워드는 공백일 수 없습니다.")
                @Size(max = 100, message = "키워드는 100자 이하로 입력해주세요.")
                        String> keywords
) {
}
