package com.springboot.monew.newsarticle.dto.response;

import com.springboot.monew.newsarticle.enums.ArticleSource;
import java.time.Instant;

// 네이버,RSS 응답을 통일해주는 DTO
public record CollectedArticle(
    ArticleSource source,
    String originalLink,
    String title,
    Instant publishedAt,
    String summary
) {

}
