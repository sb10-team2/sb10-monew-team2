package com.springboot.monew.newsarticles.service.collector;

import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.service.NaverNewsApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NaverArticleCollector implements ArticleCollector {

    //네이버
    private final NaverNewsApiClient naverNewsApiClient;

    @Override
    public ArticleSource getSource() {
        return ArticleSource.NAVER;
    }

    @Override
    public List<CollectedArticle> collect(List<String> keywords) {
        return keywords.stream()
                .flatMap(keyword -> naverNewsApiClient.searchNews(keyword).stream())
                .map(this::toCollectedArticle)
                .toList();
    }

    private CollectedArticle toCollectedArticle(NaverNewsItem item) {
        return new CollectedArticle(
                ArticleSource.NAVER,
                item.originallink(),
                cleanHtml(item.title()),
                parsePublishedAt(item.pubDate()),
                cleanHtml(item.description())
        );
    }

    //html 태그 지우는것 담당
    private String cleanHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("<.*?>", "").trim();
    }

    //네이버 API에서 오는 뉴스기사 날짜: "Thu, 16 Apr 2026 16:28:00 +0900"
    //이걸 2026-04-16T07:28:00Z 이런식으로 변경
    private Instant parsePublishedAt(String pubDate) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        return ZonedDateTime.parse(pubDate, formatter).toInstant();
    }
}
