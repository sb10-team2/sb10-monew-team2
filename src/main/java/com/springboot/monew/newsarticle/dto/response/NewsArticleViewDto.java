package com.springboot.monew.newsarticle.dto.response;

import com.springboot.monew.newsarticle.enums.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record NewsArticleViewDto (
    UUID id,
    UUID viewedBy,
    Instant createdAt,
    UUID articleId,
    ArticleSource source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    Long articleCommentCount,
    Long articleViewCount

) {}
