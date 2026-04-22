package com.springboot.monew.newsarticles.controller;

import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.service.NewsArticleCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/article")
public class NewsArticleController {

    private final NewsArticleCollectService newsArticleCollectService;

    @PostMapping
    public ResponseEntity<String> collectNews() {
        newsArticleCollectService.collectAll();
        return ResponseEntity.ok("뉴스 수집 완료");
    }
}
