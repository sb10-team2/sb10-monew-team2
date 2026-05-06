package com.springboot.monew.comment.dto;

import java.time.Instant;
import java.util.List;

// 페이지네이션 응답 Dto
public record CursorPageResponseCommentDto<T>(
    List<T> content,
    String nextCursor,
    Instant nextAfter,
    Integer size,
    Long totalElements,
    Boolean hasNext) {}
