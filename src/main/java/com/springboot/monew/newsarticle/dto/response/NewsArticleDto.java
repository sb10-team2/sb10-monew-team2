package com.springboot.monew.newsarticle.dto.response;

import com.springboot.monew.newsarticle.enums.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record NewsArticleDto(
    UUID id,
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary,
    Long commentCount,
    Long viewCount,
    Boolean viewedByMe

) {}
