package com.springboot.monew.newsarticle.service.collector;

import com.springboot.monew.newsarticle.dto.NaverNewsItem;
import com.springboot.monew.newsarticle.dto.response.CollectedArticle;
import com.springboot.monew.newsarticle.enums.ArticleSource;
import com.springboot.monew.newsarticle.exception.ArticleException;
import com.springboot.monew.newsarticle.service.NaverNewsApiClient;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

    //최종 수집 결과 저장 리스트
    List<CollectedArticle> result = new ArrayList<>();
    Boolean interrupted = false;

    //키워드별 뉴스 수집 수행
    //stream 대신 for문을 사용해서 특정 키워드 실패시 전체 배치 중단되지 않도록 처리
    for (String keyword : keywords) {
      try {

        //네이버 뉴스 API 호출
        List<NaverNewsItem> items = naverNewsApiClient.searchNews(keyword);

        // 수집한 뉴스 데이터를 내부 CollectedArticle 형태로 변환
        result.addAll(
            items.stream()
                .map(this::toCollectedArticle)
                .filter(article -> {
                  boolean valid = article.publishedAt() != null;

                  if (!valid) {

                    log.warn("publishedAt 없음으로 기사 제외 - originallink={}", article.originalLink());
                  }
                  return valid;
                })
                .toList()
        );


      } catch (ArticleException e) {
        log.warn("키워드 뉴스 수집 실패 - keyword={}", keyword, e);

      } finally {

        //성공, 실패 여부와 관계없이 요청간 간격을 둔다.
        try {

          // 네이버 API 속도 제한 방지
          // 네이버 API 속도제한(429 Too Many Requests) 방지용 sleep
          Thread.sleep(200);
        }catch (InterruptedException e) {
          log.warn("뉴스 수집 sleep 중 interrupt 발생");
          Thread.currentThread().interrupt(); // 인터럽트 상태 복원
          interrupted = true;
        }
      }
      // finally 밖에서 loop 종료
      if (interrupted) {
        break;
      }
    }
    // 동일 기사 중복 제거
    List<CollectedArticle> distinctResult = result.stream()
            .distinct()
            .toList();

    log.info("네이버 뉴스 수집 종료 - 수집된 기사 수={}", distinctResult.size());

    return distinctResult;
  }

  private CollectedArticle toCollectedArticle(NaverNewsItem item) {
    Instant publishedAt = parsePublishedAt(item.pubDate());

    if (publishedAt == null) {
      log.warn("pubDate 파싱 실패 - originallink={}, pubDate={}", item.originallink(), item.pubDate());
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