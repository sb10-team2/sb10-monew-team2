package com.springboot.monew.user.outbox.payload.articleview;

import com.springboot.monew.newsarticle.enums.ArticleSource;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
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

  public static ArticleViewedPayload of(UUID userId, ArticleViewItem item) {
    return new ArticleViewedPayload(
        userId,
        item.id(),
        item.createdAt(),
        item.articleId(),
        item.source(),
        item.sourceUrl(),
        item.articleTitle(),
        item.articlePublishedDate(),
        item.articleSummary(),
        item.articleCommentCount(),
        item.articleViewCount()
    );
  }
}
