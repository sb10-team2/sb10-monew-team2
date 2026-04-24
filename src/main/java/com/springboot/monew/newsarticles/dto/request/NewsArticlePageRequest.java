package com.springboot.monew.newsarticles.dto.request;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.enums.NewsArticleDirection;
import com.springboot.monew.newsarticles.enums.NewsArticleOrderBy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NewsArticlePageRequest(

    //검색어(제목, 요약)
    String keyword,

    //관심사ID
    UUID interestId,

    //출처 필터
    List<ArticleSource> sourceIn,

    //날짜 범위
    Instant publishDateFrom,
    Instant publishDateTo,

    //정렬
    @NotNull NewsArticleOrderBy orderBy,
    @NotNull NewsArticleDirection direction,

    //커서 기반 페이징
    // after는 Swagger 스펙 상 별도 파라미터로 정의되어 있으나,
    // 실제 페이지네이션 조회 시에는 cursor 파라미터 내부에 "cursor|after" 형태로 인코딩되어 전달됩니다.
    String cursor,
    String after,

    //페이지 크기
    @NotNull @Min(1) @Max(100) Integer limit

) {

}
