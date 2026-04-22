package com.springboot.monew.newsarticles.entity;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor //기본 생성자
@Table(name = "news_articles",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_NEWS_ARTICLES_ORIGINAL_LINK", columnNames = "original_link")
        }
)
public class NewsArticle extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleSource source;

    @Column(name = "original_link", nullable = false, length = 500)
    private String originalLink;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(columnDefinition = "TEXT")    //JPA가 테이블 생성할때 해당 컬럼을 DB의 TEXT 타입으로 지정하라.
    private String summary;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Builder
    public NewsArticle(ArticleSource source, String originalLink, String title, Instant publishedAt, String summary) {
        this.source = source;
        this.originalLink = originalLink;
        this.title = title;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.viewCount = 0L;
        this.isDeleted = false;
    }

//    // ===== 비즈니스 로직 =====
//    public void increaseViewCount() {
//        this.viewCount++;
//    }
//
//    public void delete() {
//        this.isDeleted = true;
//    }


}
