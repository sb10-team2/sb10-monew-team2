package com.springboot.monew.interest.dto.response;

import java.time.Instant;
import java.util.List;

// 커서 기반 관심사 페이지 응답 dto
public record CursorPageResponseInterestDto(
    List<InterestDto> content,
    String nextCursor,
    Instant nextAfter,
    int size,
    long totalElements,
    boolean hasNext
) {

}
