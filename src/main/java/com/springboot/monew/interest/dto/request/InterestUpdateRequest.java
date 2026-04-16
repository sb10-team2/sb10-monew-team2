package com.springboot.monew.interest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// 관심사 수정 요청 dto
public record InterestUpdateRequest(
        @NotEmpty
        @Size(min = 1, max = 10)
        List<@NotBlank @Size(max = 100) String> keywords
) {
}
