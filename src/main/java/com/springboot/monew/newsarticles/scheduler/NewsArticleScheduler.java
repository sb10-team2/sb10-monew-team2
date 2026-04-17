package com.springboot.monew.newsarticles.scheduler;

import com.springboot.monew.newsarticles.service.NewsArticleCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleScheduler {

    private final NewsArticleCollectService newsArticleCollectService;

    //매 시간 0분 0초 newsArticleCollectService.collectAll()을 실행한다.
    @Scheduled(cron = "0 0 * * * *")
    public void collectArticlesEveryHour() {

        log.info("뉴스 기사 수집 배치 시작");
        newsArticleCollectService.collectAll();
        log.info("뉴스 기사 수집 배치 종료");
    }
}
