package com.springboot.monew.newsarticle.dto;

import com.springboot.monew.newsarticle.enums.ArticleSource;
import java.time.Instant;
import java.util.UUID;

//S3에 백업할때 올릴 뉴스기사DTO
public record NewsArticleBackupDto(
    UUID id,
    ArticleSource source,
    String originalLink,
    String title,
    Instant publishedAt,
    String summary,
    Boolean isDeleted,
    Instant createdAt

) {

}
