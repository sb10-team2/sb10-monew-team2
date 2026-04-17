package com.springboot.monew.newsarticles.scheduler;

import com.springboot.monew.newsarticles.service.NewsArticleCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsArticleScheduler {

    private final NewsArticleCollectService newsArticleCollectService;

    @Scheduled(cron = "0 0 * * * *")
    public void collectArticlesEveryHour() {
        newsArticleCollectService.collectAll();
    }
}
