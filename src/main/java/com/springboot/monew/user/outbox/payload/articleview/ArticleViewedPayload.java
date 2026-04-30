package com.springboot.monew.user.outbox.payload.articleview;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewedPayload(
    UUID userId,
    UUID articleViewId,
    Instant createdAt,
    UUID articleId,
    ArticleSource source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    Long articleCommentCount,
    Long articleViewCount
) {

}
