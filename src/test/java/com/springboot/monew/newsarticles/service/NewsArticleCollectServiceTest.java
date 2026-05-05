package com.springboot.monew.newsarticles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.springboot.monew.interest.dto.response.InterestKeywordInfo;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.newsarticles.dto.CollectedArticleWithInterest;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.metric.result.NewsArticleCollectResult;
import com.springboot.monew.newsarticles.metric.result.NewsArticleSaveResult;
import com.springboot.monew.newsarticles.service.collector.ArticleCollector;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsArticleCollectServiceTest {

  @Mock
  private ArticleCollector collector;

  @Mock
  private NewsArticleService newsArticleService;

  @Mock
  private InterestKeywordRepository interestKeywordRepository;

  private NewsArticleCollectService newsArticleCollectService;

  @BeforeEach
  void setUp() {
    newsArticleCollectService = new NewsArticleCollectService(
        List.of(collector),
        newsArticleService,
        interestKeywordRepository
    );
  }

  @Test
  @DisplayName("전체 뉴스 수집 - 키워드와 매칭된 기사만 저장한다")
  void collectAll_SavesOnlyMatchedArticles_WhenArticlesCollected() {

    // given
    UUID interestId = UUID.randomUUID();

    given(interestKeywordRepository.findAllInterestKeywordInfos())
        .willReturn(List.of(
            new InterestKeywordInfo(interestId, "AI")
        ));

    given(collector.getSource()).willReturn(ArticleSource.NAVER);

    CollectedArticle matchedArticle = new CollectedArticle(
        ArticleSource.NAVER,
        "https://news.com/1",
        "AI 기술 발전",
        Instant.now(),
        "인공지능 관련 기사 요약"
    );

    CollectedArticle unmatchedArticle = new CollectedArticle(
        ArticleSource.NAVER,
        "https://news.com/2",
        "스포츠 뉴스",
        Instant.now(),
        "축구 경기 결과"
    );

    given(collector.collect(List.of("ai")))
        .willReturn(List.of(matchedArticle, unmatchedArticle));

    given(newsArticleService.saveAll(anyList()))
        .willReturn(new NewsArticleSaveResult(
            2, // 요청 기사 수
            2, // distinct
            0, // 기존 없음
            1, // 저장된 기사
            1  // 관심사 연결
        ));

    // when
    NewsArticleCollectResult result = newsArticleCollectService.collectAll();

    // then
    assertThat(result.keywordCount()).isEqualTo(1);
    assertThat(result.sourceResults()).hasSize(1);

    ArgumentCaptor<List<CollectedArticleWithInterest>> captor =
        ArgumentCaptor.forClass(List.class);

    verify(newsArticleService).saveAll(captor.capture());

    List<CollectedArticleWithInterest> savedArticles = captor.getValue();

    // 핵심 검증: 매칭된 기사만 저장됨
    assertThat(savedArticles).hasSize(1);
    assertThat(savedArticles.get(0).article()).isEqualTo(matchedArticle);
    assertThat(savedArticles.get(0).interestIds()).containsExactly(interestId);
  }

  @Test
  @DisplayName("전체 뉴스 수집 - 키워드가 없으면 저장 대상 없이 수집을 수행한다")
  void collectAll_DoesNotSaveMatchedArticles_WhenKeywordIsEmpty() {

    // given
    // 관심사 키워드가 하나도 없는 상황
    given(interestKeywordRepository.findAllInterestKeywordInfos())
        .willReturn(List.of());

    given(collector.getSource()).willReturn(ArticleSource.NAVER);

    // 키워드가 없으므로 collector에는 빈 키워드 리스트가 전달된다.
    given(collector.collect(List.of()))
        .willReturn(List.of(
            new CollectedArticle(
                ArticleSource.NAVER,
                "https://news.com/1",
                "AI 뉴스",
                Instant.now(),
                "요약"
            )
        ));

    // 키워드가 없어 매칭된 기사가 없으므로 저장 결과는 empty로 처리
    given(newsArticleService.saveAll(anyList()))
        .willReturn(NewsArticleSaveResult.empty());

    // when
    NewsArticleCollectResult result = newsArticleCollectService.collectAll();

    // then
    assertThat(result.keywordCount()).isEqualTo(0);
    assertThat(result.sourceResults()).hasSize(1);

    // saveAll에 전달된 저장 대상 목록을 캡처
    ArgumentCaptor<List<CollectedArticleWithInterest>> captor =
        ArgumentCaptor.forClass(List.class);

    verify(newsArticleService).saveAll(captor.capture());

    // 키워드가 없으면 어떤 기사도 관심사와 매칭되지 않으므로 빈 리스트가 저장 요청된다.
    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  @DisplayName("전체 뉴스 수집 - collector에서 예외가 발생하면 실패 결과를 반환하고 저장하지 않는다")
  void collectAll_ReturnsFailureResult_WhenCollectorThrowsException() {

    // given
    UUID interestId = UUID.randomUUID();

    // 관심사 키워드가 존재하는 상황
    given(interestKeywordRepository.findAllInterestKeywordInfos())
        .willReturn(List.of(
            new InterestKeywordInfo(interestId, "AI")
        ));

    given(collector.getSource()).willReturn(ArticleSource.NAVER);

    // 외부 수집기 호출 중 예외가 발생하는 상황
    given(collector.collect(List.of("ai")))
        .willThrow(new RuntimeException("수집 실패"));

    // when
    NewsArticleCollectResult result = newsArticleCollectService.collectAll();

    // then
    assertThat(result.keywordCount()).isEqualTo(1);
    assertThat(result.sourceResults()).hasSize(1);

    // collector 단계에서 실패했으므로 saveAll은 호출되지 않아야 한다.
    verify(newsArticleService, never()).saveAll(anyList());
  }
}