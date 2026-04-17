package com.springboot.monew.newsarticles.service;

import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;

    @Transactional
    public void saveAll(List<CollectedArticle> articles) {

        //기사 하나마다 existsByOriginalLink()로 DB조회 1번씩 하게됨.
        //ex) 100개 기사 -> 100번 existsByOriginalLink 조회.
        //개선 필요.
        List<NewsArticle> entities = articles.stream()
                .filter(article -> !newsArticleRepository.existsByOriginalLink(article.originalLink()))
                .map(this::toEntity)
                .toList();

        newsArticleRepository.saveAll(entities);

    }

    //DB저장을 위해 NewsArticle Entity형으로 변환
    private NewsArticle toEntity(CollectedArticle article) {
        return NewsArticle.builder()
                .source(article.source())
                .originalLink(article.originalLink())
                .title(article.title())
                .publishedAt(article.publishedAt())
                .summary(article.summary())
                .build();
    }
}
