package com.springboot.monew.newsarticle.service.collector;

import com.springboot.monew.newsarticle.dto.response.CollectedArticle;
import com.springboot.monew.newsarticle.enums.ArticleSource;
import com.springboot.monew.newsarticle.service.RssClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//한국경제 뉴스기사 수집기
@Slf4j
@Service
@RequiredArgsConstructor
public class HankyungRssCollector implements ArticleCollector {

  private final RssClient rssClient;

  @Override
  public ArticleSource getSource() {
    return ArticleSource.HANKYUNG;
  }

  //네이버 API처럼 키워드 검색은 아니다.
  //일단 가져오고, NewsArticleCollectService에서 키워드 필터링한다.
  @Override
  public List<CollectedArticle> collect(List<String> keywords) {
    List<CollectedArticle> result = rssClient.read("https://www.hankyung.com/feed/all-news")
        .stream()
        .filter(item -> item.link() != null && !item.link().isBlank())
        .map(item -> new CollectedArticle(
            ArticleSource.HANKYUNG,
            item.link(),
            item.title(),
            item.publishedAt(),
            item.description()
        ))
        .toList();

    log.info("한국경제 수집 개수= {}", result.size());

    return result;
  }
}
