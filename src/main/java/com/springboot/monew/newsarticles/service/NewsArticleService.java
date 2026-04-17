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
    public void saveAll(List<CollectedArticle> collectedArticles) {
        for (CollectedArticle item : collectedArticles) {
            if (newsArticleRepository.existsByOriginalLink(item.originalLink())) {
                continue;
            }

            NewsArticle article = new NewsArticle(
                    item.source().name(),
                    item.originalLink(),
                    item.title(),
                    item.publishedAt(),
                    item.summary()
            );

            newsArticleRepository.save(article);
        }
    }
}
