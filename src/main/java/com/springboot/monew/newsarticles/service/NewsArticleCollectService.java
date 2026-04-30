package com.springboot.monew.newsarticles.service;

import com.springboot.monew.interest.dto.response.InterestKeywordInfo;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.newsarticles.metric.result.NewsArticleCollectResult;
import com.springboot.monew.newsarticles.metric.result.NewsArticleSaveResult;
import com.springboot.monew.newsarticles.metric.result.NewsArticleSourceCollectResult;
import com.springboot.monew.newsarticles.dto.CollectedArticleWithInterest;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.service.collector.ArticleCollector;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//전체 수집 실행용 서비스
//네이버 수집, 연합뉴스 수집, 한국경제 수집...을 한번에 실행
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleCollectService {

  //네이버수집기,연합뉴스수집기등이 List 형태로 들어간다.
  private final List<ArticleCollector> collectors;
  private final NewsArticleService newsArticleService;
  private final InterestKeywordRepository interestKeywordRepository;

  //전체에서 수집
  //단 하나의 외부 API만 느린 응답이 있어도 DB 트랜잭션이 길게 유지되어 DB연결을 불필요하게 점유한다.
  //따라서 newsArticleService.saveAll안에서 트랜잭션 적용하는게 맞다.
  //@Transactional
  public NewsArticleCollectResult collectAll() {

    //(관심사 id, 키워드) 리스트
    List<InterestKeywordInfo> infos = interestKeywordRepository.findAllInterestKeywordInfos();

    //keyword -> interestIds
    //{"키워드" -> [관심사 ID]}식으로 변경
    // 하나의 키워드가 여러 관심사에 연결될 수도 있다.
    // "ai" ->[기술, 스타트업, 투자] 이런식으로 ..
    Map<String, Set<UUID>> keywordToInterestIds = infos.stream()
        .collect(Collectors.groupingBy(
            info -> info.keywordName().toLowerCase(),
            Collectors.mapping(InterestKeywordInfo::interestId,
                Collectors.toSet())     //interestId 꺼낸다음에 Set으로 모아라.
        ));

    //키워드 리스트 추출
    List<String> keywords = new ArrayList<>(keywordToInterestIds.keySet());

    log.info("키워드 리스트={}", keywords);

    List<NewsArticleSourceCollectResult> sourceResults = new ArrayList<>();

    for (ArticleCollector collector : collectors) {
      Instant startedAt = Instant.now();

      try {
        List<CollectedArticle> collectedArticles = collector.collect(keywords);

        List<CollectedArticleWithInterest> matchedArticles = collectedArticles.stream()
            .map(article -> {
              Set<UUID> matchedInterestIds = findMatchedInterestIds(article,
                  keywordToInterestIds);

              return new CollectedArticleWithInterest(article, matchedInterestIds);
            })
            .filter(item -> !item.interestIds().isEmpty())
            .toList();

        NewsArticleSaveResult saveResult = newsArticleService.saveAll(matchedArticles);
        sourceResults.add(NewsArticleSourceCollectResult.success(collector.getSource(),
            collectedArticles.size(), matchedArticles.size(), saveResult,
            Duration.between(startedAt, Instant.now())));
      } catch (Exception e) {
        sourceResults.add(NewsArticleSourceCollectResult.failure(collector.getSource(),
            Duration.between(startedAt, Instant.now())));
        log.error("기사 수집 실패 source={}", collector.getSource(), e);
      }

    }

    return new NewsArticleCollectResult(keywords.size(), sourceResults);
  }

  private Set<UUID> findMatchedInterestIds(
      CollectedArticle article,
      Map<String, Set<UUID>> keywordToInterestIds
  ) {
    //text = 제목 + 요약 소문자로 합친 문자열
    String text = (article.title() + " " + article.summary()).toLowerCase();

    //"손흥민" -> [축구, 운동선수] 이런식으로 키워드 -> [관심사1, 관심사2 ..] 이런형태
    //제목과 요약에 해당 키워드가 있으면 그 article은 관심사1, 관심사 2 ... 이런식으로 매칭이 된다.
    // (기사1, [관심사1]), (기사2, [관심사1, 관심사2]) 이런식의 결과 return
    return keywordToInterestIds.entrySet().stream()
        .filter(entry -> text.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toSet());
  }
}
