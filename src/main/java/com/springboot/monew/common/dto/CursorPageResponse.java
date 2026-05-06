package com.springboot.monew.common.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;

@Builder
public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    String nextAfter,
    int size,
    long totalElements,
    boolean hasNext)
    implements Serializable {

}
