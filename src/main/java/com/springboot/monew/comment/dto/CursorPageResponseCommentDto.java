package com.springboot.monew.comment.dto;

import java.time.Instant;
import java.util.List;

// 페이지네이션 응답 Dto
// Todo: 디폴트 값 설정 추가
public record CursorPageResponseCommentDto<T>(
    List<T> content,
    String nextCursor,
    Instant nextAfter,
    int size,
    int totalElements,
    boolean hasNext) {}
