package com.springboot.monew.newsarticles.service;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.newsarticles.dto.CollectedArticleWithInterest;
import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.exception.ArticleException;
import com.springboot.monew.newsarticles.exception.NewsArticleErrorCode;
import com.springboot.monew.newsarticles.mapper.NewsArticleMapper;
import com.springboot.monew.newsarticles.repository.ArticleInterestRepository;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.notification.event.InterestNotificationEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleService {

  private final NewsArticleRepository newsArticleRepository;
  private final NewsArticleMapper newsArticleMapper;
  private final InterestRepository interestRepository;
  private final ArticleInterestRepository articleInterestRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void saveAll(List<CollectedArticleWithInterest> articlesWithInterests) {

    if (articlesWithInterests == null || articlesWithInterests.isEmpty()) {
      return;
    }

    // 1. originalLink가 유효한 데이터만 남기고,
    // 같은 originalLink는 interestIds를 합쳐서 하나로 만든다.
    List<CollectedArticleWithInterest> distinctItems = new ArrayList<>(
        articlesWithInterests.stream()
            .filter(item -> item.article() != null)
            .filter(item -> item.article().originalLink() != null && !item.article().originalLink()
                .isBlank())
            .collect(Collectors.toMap(
                item -> item.article().originalLink(),
                item -> item,
                (existing, replacement) -> new CollectedArticleWithInterest(
                    existing.article(),
                    Stream.concat(
                            existing.interestIds().stream(),
                            replacement.interestIds().stream()
                        )
                        .collect(Collectors.toSet())
                )
            ))
            .values()
    );

    if (distinctItems.isEmpty()) {
      return;
    }

    // 2. 요청에 들어온 originalLink 목록
    Set<String> originalLinks = distinctItems.stream()
        .map(item -> item.article().originalLink())
        .collect(Collectors.toSet());

    // 3. DB에 이미 존재하는 기사 조회
    List<NewsArticle> existingArticles = newsArticleRepository.findAllByOriginalLinkIn(
        originalLinks);

    Map<String, NewsArticle> existingArticleMap = existingArticles.stream()
        .collect(Collectors.toMap(
            NewsArticle::getOriginalLink,
            article -> article,
            (left, right) -> left
        ));

    Set<String> existingLinks = existingArticleMap.keySet();

    // 4. DB에 없는 신규 기사만 엔티티로 변환 후 저장
    List<NewsArticle> newArticles = distinctItems.stream()
        .filter(item -> !existingLinks.contains(item.article().originalLink()))
        .map(item -> newsArticleMapper.toEntity(item.article()))
        .toList();

    List<NewsArticle> savedArticles = newArticles.isEmpty()
        ? List.of()
        : newsArticleRepository.saveAll(newArticles);

    // 5. 기존 기사 + 신규 저장 기사 모두 합쳐서 originalLink -> NewsArticle 맵 생성
    Map<String, NewsArticle> allArticleMap = Stream.concat(existingArticles.stream(),
            savedArticles.stream())
        .collect(Collectors.toMap(
            NewsArticle::getOriginalLink,
            article -> article,
            (left, right) -> left
        ));

    // 6. 이번 요청에서 사용될 모든 기사 엔티티 목록
    List<NewsArticle> targetArticles = distinctItems.stream()
        .map(item -> allArticleMap.get(item.article().originalLink()))
        .filter(article -> article != null)
        .toList();

    if (targetArticles.isEmpty()) {
      return;
    }

    // 7. 이미 존재하는 article_interest 연결 조회
    List<ArticleInterest> existingArticleInterests =
        articleInterestRepository.findAllByNewsArticleIn(targetArticles);

    // (newsArticleId, interestId) 조합을 문자열 키로 만들어 중복 확인용 Set 생성
    Set<String> existingRelationKeys = existingArticleInterests.stream()
        .map(ai -> buildRelationKey(
            ai.getNewsArticle().getId(),
            ai.getInterest().getId()
        ))
        .collect(Collectors.toSet());

    // 8. 새로 저장할 article_interests 생성
    List<ArticleInterest> newArticleInterests = distinctItems.stream()
        .flatMap(item -> {
          NewsArticle article = allArticleMap.get(item.article().originalLink());

          if (article == null) {
            return Stream.empty();
          }

          return item.interestIds().stream()
              .map(interestId -> {
                String relationKey = buildRelationKey(article.getId(), interestId);

                if (existingRelationKeys.contains(relationKey)) {
                  return null;
                }

                Interest interestRef = interestRepository.getReferenceById(interestId);
                return new ArticleInterest(article, interestRef);
              })
              .filter(articleInterest -> articleInterest != null);
        })
        .toList();

    if (!newArticleInterests.isEmpty()) {
      articleInterestRepository.saveAll(newArticleInterests);
      eventPublisher.publishEvent(InterestNotificationEvent.from(newArticleInterests));
    }

    log.info("뉴스기사 저장 완료 - 신규 기사 수: {}, 신규 기사-관심사 연결 수: {}",
        savedArticles.size(),
        newArticleInterests.size());
  }

  private String buildRelationKey(Object articleId, Object interestId) {
    return articleId + ":" + interestId;
  }

  // 뉴스기사 물리 삭제
  // 뉴스기사 관련된 모든 row 삭제 -> DB 제약조건에 따라 삭제
  @Transactional
  public void hardDelete(UUID articleId) {

    NewsArticle newsArticle = getNewsArticle(articleId);
    newsArticleRepository.delete(newsArticle);
    log.info("뉴스기사 물리 삭제 완료 - articleId: {}", articleId);
  }

  // 뉴스기사 논리 삭제
  @Transactional
  public void softDelete(UUID articleId) {

    NewsArticle newsArticle = getNewsArticle(articleId);

    if (newsArticle.isDeleted()) {
      throw new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED,
          Map.of("articleId", articleId));
    }
    newsArticle.delete();
    log.info("뉴스기사 논리 삭제 완료 - articleId: {}", articleId);

  }

  private NewsArticle getNewsArticle(UUID articleId) {
    return newsArticleRepository.findById(articleId).orElseThrow(
        () -> new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND,
            Map.of("articleId", articleId)));
  }
}
