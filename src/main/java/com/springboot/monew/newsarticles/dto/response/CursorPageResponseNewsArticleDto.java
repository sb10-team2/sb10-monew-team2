package com.springboot.monew.newsarticles.dto.response;

import java.util.List;

// 커서 기반 뉴스기사 페이지 응답 DTO
public record CursorPageResponseNewsArticleDto(
    List<NewsArticleDto> content,
    String nextCursor,
    String nextAfter,
    int size,
    long totalElements,
    boolean hasNext
) {


}
