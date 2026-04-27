package com.springboot.monew.newsarticles.service.collector;

import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.service.NaverNewsApiClient;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverArticleCollector implements ArticleCollector {

  private final NaverNewsApiClient naverNewsApiClient;

  @Override
  public ArticleSource getSource() {
    return ArticleSource.NAVER;
  }

  @Override
  public List<CollectedArticle> collect(List<String> keywords) {

    log.info("네이버 뉴스 수집 시작 - keywords={}", keywords);

    List<CollectedArticle> result = keywords.stream()
        .flatMap(keyword -> naverNewsApiClient.searchNews(keyword).stream())
        .map(this::toCollectedArticle)
        .filter(article -> {
          boolean valid = article.publishedAt() != null;
          if (!valid) {
            log.warn("publishedAt 없음으로 기사 제외 - originallink={}", article.originalLink());
          }
          return valid;
        })
        .distinct()
        .toList();

    log.info("네이버 뉴스 수집 종료 - 수집된 기사 수={}", result.size());

    return result;
  }

  private CollectedArticle toCollectedArticle(NaverNewsItem item) {
    Instant publishedAt = parsePublishedAt(item.pubDate());

    if (publishedAt == null) {
      log.warn(
          "pubDate 파싱 실패 - originallink={}, pubDate={}",
          item.originallink(),
          item.pubDate()
      );
    }

    return new CollectedArticle(
        ArticleSource.NAVER,
        item.originallink(),
        cleanHtml(item.title()),
        publishedAt,
        cleanHtml(item.description())
    );
  }

  private String cleanHtml(String text) {
    if (text == null) {
      return "";
    }
    return text.replaceAll("<.*?>", "").trim();
  }

  private Instant parsePublishedAt(String pubDate) {
    if (pubDate == null || pubDate.isBlank()) {
      log.warn("pubDate 없음 - pubDate={}", pubDate);
      return null;
    }

    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    try {
      return ZonedDateTime.parse(pubDate, formatter).toInstant();
    } catch (DateTimeParseException e) {
      log.warn("pubDate 파싱 실패 - pubDate={}", pubDate, e);
      return null;
    }
  }
}
