package com.springboot.monew.newsarticles.dto.response;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import java.time.Instant;
import java.util.UUID;

//내부 조회용 DTO
//NewsArticle을 return으로  쓰자니 CommentCount와 Interest가 없다.
//NewsArticleDto를 return으로 쓰자니 createdAt이 없다.
public record NewsArticleCursorRow(
    UUID id,
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary,
    Long commentCount,
    Long viewCount,
    Boolean viewedByMe,
    Instant createdAt
) {

  public NewsArticleDto toDto() {
    return new NewsArticleDto(
        id,
        source,
        sourceUrl,
        title,
        publishDate,
        summary,
        commentCount,
        viewCount,
        viewedByMe
    );
  }
}
