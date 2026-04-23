package com.springboot.monew.newsarticles.dto.response;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record NewsArticleDto(
    UUID id,
    ArticleSource source,
    String sorceUrl,
    String title,
    Instant publishDate,
    String summary,
    Long commentCount,
    Long viewCount,
    Boolean viewedByMe

) {}
