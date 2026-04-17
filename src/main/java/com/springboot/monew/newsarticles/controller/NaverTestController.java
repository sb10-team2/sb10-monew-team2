package com.springboot.monew.newsarticles.controller;

import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import com.springboot.monew.newsarticles.service.NaverNewsApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test/naver")
@RequiredArgsConstructor
public class NaverTestController {

    private final NaverNewsApiClient naverNewsApiClient;

    //네이버 API 뉴스 수집 TEST
    @GetMapping
    public List<NaverNewsItem> test(@RequestParam String query) {
        return  naverNewsApiClient.searchNews(query);
    }

}
