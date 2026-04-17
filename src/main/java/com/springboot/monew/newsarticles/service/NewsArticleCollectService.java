package com.springboot.monew.newsarticles.service;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.newsarticles.service.collector.ArticleCollector;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//전체 수집 실행용 서비스
//네이버 수집, 연합뉴스 수집, 한국경제 수집...을 한번에 실행
@Service
@RequiredArgsConstructor
public class NewsArticleCollectService {

    //네이버수집기,연합뉴스수집기등이 List 형태로 들어간다.
    private final List<ArticleCollector> collectors;
    private final NewsArticleService newsArticleService;
    private final InterestKeywordRepository interestKeywordRepository;

    //전체에서 수집
    @Transactional
    public void collectAll() {

        //키워드를 List로 만든다.
        List<String> keywords = interestKeywordRepository.findAllKeywords();

        //키워드가 들어가있는 뉴스기사를 수집해서 List로 저장한다.
        for (ArticleCollector collector : collectors) {
            List<CollectedArticle> collectedArticles = collector.collect(keywords);

            List<CollectedArticle> filteredArticles = collectedArticles.stream()
                            .filter(article -> containsAnyKeyword(article, keywords))       //containsAnyKeyword에서 true인것만 남겨지게 된다.
                            .toList();

            newsArticleService.saveAll(filteredArticles);
        }
    }

    //title이나, 내용에 keyword가 있는지 check
    private boolean containsAnyKeyword(CollectedArticle article, List<String> keywords) {
        String title = article.title() == null ? "" : article.title().toLowerCase();
        String content = article.summary() == null ? "" : article.summary().toLowerCase();

        return keywords.stream()
                .map(String::toLowerCase)
                .anyMatch(keyword -> title.contains(keyword) || content.contains(keyword));
    }


}
