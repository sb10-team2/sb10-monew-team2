package com.springboot.monew.interest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

// 관심사 수정 요청 dto
public record InterestUpdateRequest(
    @NotEmpty(message = "키워드는 최소 1개 이상 입력해야 합니다.")
    @Size(min = 1, max = 10, message = "키워드는 최대 10개까지 입력할 수 있습니다.")
    List<
        @NotBlank(message = "키워드는 공백일 수 없습니다.")
        @Size(max = 100, message = "키워드는 100자 이하로 입력해주세요.")
            String> keywords
) {

}
