package com.springboot.monew.newsarticles.service;

import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import com.springboot.monew.newsarticles.dto.response.NaverNewsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class NaverNewsApiClient {
    private final RestClient restClient;

    @Value("${naver.api.key}")
    private String clientId;

    @Value("${naver.api.secret}")
    private String clientSecret;

    public NaverNewsApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<NaverNewsItem> searchNews(String query) {
        NaverNewsResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("openapi.naver.com")
                        .path("/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", 10)      //한번에 가져올 개수: 10
                        .queryParam("start", 1)         //시작위치
                        .queryParam("sort", "date")     //날짜순으로 내림차순 정렬
                        .build()
                )
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .body(NaverNewsResponse.class);

        return response == null || response.articles() == null ? List.of() : response.articles();
    }


}
