package com.springboot.monew.interest.dto.request;

import com.springboot.monew.interest.entity.InterestDirection;
import com.springboot.monew.interest.entity.InterestOrderBy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

// 관심사 목록 조회 요청 dto
public record InterestPageRequest(
    String keyword,
    @NotNull InterestOrderBy orderBy,
    @NotNull InterestDirection direction,
    String cursor,
    Instant after,
    @NotNull @Min(1) @Max(100) Integer limit
) {

}
