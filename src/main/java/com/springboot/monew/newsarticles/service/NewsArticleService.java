package com.springboot.monew.newsarticles.service;

import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.mapper.NewsArticleMapper;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;
    private final NewsArticleMapper newsArticleMapper;

    @Transactional
    public void saveAll(List<CollectedArticle> articles) {

        // 1. 같은 요청 안에서 originalLink 기준 중복 제거
        List<CollectedArticle> distinctArticles = articles.stream()
                .filter(article -> article.originalLink() != null && !article.originalLink().isBlank())
                .collect(Collectors.toMap(
                        CollectedArticle::originalLink,
                        article -> article,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .toList();

        // 2. DB에 이미 존재하는 링크 제거
        List<NewsArticle> entities = distinctArticles.stream()
            .filter(article -> !newsArticleRepository.existsByOriginalLink(article.originalLink()))
            .map(newsArticleMapper::toEntity)
            .toList();

        newsArticleRepository.saveAll(entities);
    }


}
