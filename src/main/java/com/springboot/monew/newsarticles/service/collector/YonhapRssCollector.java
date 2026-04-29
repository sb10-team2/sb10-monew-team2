package com.springboot.monew.newsarticles.service.collector;

import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.service.RssClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//연합뉴스 수집기
@Slf4j
@Service
@RequiredArgsConstructor
public class YonhapRssCollector implements ArticleCollector {

  private final RssClient rssClient;

  @Override
  public ArticleSource getSource() {
    return ArticleSource.YEONHAP;
  }

  //네이버 API처럼 키워드 검색은 아니다.
  //일단 가져오고, NewsArticleCollectService에서 키워드 필터링한다.
  @Override
  public List<CollectedArticle> collect(List<String> keywords) {
    List<CollectedArticle> result = rssClient.read("http://www.yonhapnewstv.co.kr/browse/feed/")
        .stream()
        .filter(item -> item.link() != null && !item.link().isBlank())
        .map(item -> new CollectedArticle(
            ArticleSource.YEONHAP,
            item.link(),
            item.title(),
            item.publishedAt(),
            item.description()
        ))
        .toList();

    log.info("연합뉴스 수집 개수= {}", result.size());

    return result;
  }

}
