package com.springboot.monew.newsarticles.service.collector;

import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;

import java.util.List;

//기사 수집 방법을 추상화
public interface ArticleCollector {
    //기사 출처
    ArticleSource getSource();

    //키워드 리스트를 넣고 기사 수집
    List<CollectedArticle> collect(List<String> keywords);
}
