package com.springboot.monew.newsarticles.service;

import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.newsarticles.service.collector.ArticleCollector;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//전체 수집 실행용 서비스
@Service
@RequiredArgsConstructor
public class NewsArticleCollectService {

    private final List<ArticleCollector> collectors;
    private final NewsArticleService newsArticleService;
    private final InterestRepository interestRepository;

    @Transactional
    public void collectAll() {
        List<String> keywords = interestRepository.findAllKeywords();

        for (ArticleCollector collector : collectors) {
            List<CollectedArticle> collectedArticles = collector.collect(keywords);
            newsArticleService.saveAll(collectedArticles);
        }
    }
}
