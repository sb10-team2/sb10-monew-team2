package com.springboot.monew.newsarticles.service;

import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.newsarticles.dto.CollectedArticleWithInterest;
import com.springboot.monew.newsarticles.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticles.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleCursorRow;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.entity.ArticleInterest;
import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.exception.ArticleException;
import com.springboot.monew.newsarticles.exception.NewsArticleErrorCode;
import com.springboot.monew.newsarticles.mapper.NewsArticleMapper;
import com.springboot.monew.newsarticles.mapper.NewsArticleViewMapper;
import com.springboot.monew.newsarticles.repository.ArticleInterestRepository;
import com.springboot.monew.newsarticles.repository.ArticleViewRepository;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.notification.event.InterestNotificationEvent;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.repository.UserRepository;
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
  private final ArticleViewRepository articleViewRepository;
  private final InterestRepository interestRepository;
  private final ArticleInterestRepository articleInterestRepository;
  private final UserRepository userRepository;

  private final NewsArticleMapper newsArticleMapper;
  private final NewsArticleViewMapper newsArticleViewMapper;
  private final CommentRepository commentRepository;
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

  //뉴스기사 뷰(조회 이력) 등록
  //뉴스기사 클릭시, 조회이력 1건 생성 + 기사 조회수 1 증가
  @Transactional
  public NewsArticleViewDto createView(UUID articleId, UUID userId){

    //사용자 유효성 검증
    validateActiveUser(userId);

    //뉴스기사 존재 확인
    NewsArticle newsArticle = getNewsArticle(articleId);

    //논리 삭제된 뉴스기사의 경우 예외처리
    if(newsArticle.isDeleted()){
      throw new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED, Map.of("articleId", articleId));
    }

    // 이미 본 기사인지 확인
    // ToDo: 동시에 existsByNewsArticleIdAndUserId false받고, save하면 어떻게 되나? -> DB 유니크 제약 위반 발생
    if (articleViewRepository.existsByNewsArticleIdAndUserId(articleId, userId)) {
      throw new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_VIEWED, Map.of("articleId", articleId, "userId", userId));
    }

    ArticleView savedArticleView;

    try{
      //save() : 영속성 컨텍스트에만 저장, 실제 insert는 commit 시점.
      //saveAndFlush() : 즉시 DB에 insert
      //요청 A, 요청 B 동시에 요청이 들어왔다 -> A랑 B중 누구든지간에 saveAndFlush()를 먼저 하는 요청이 있을거라 DB 레벨에서 UNIQUE 조건에 의해 예외 발생될것이다.
      savedArticleView = articleViewRepository.saveAndFlush(new ArticleView(newsArticle, user));
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_VIEWED, Map.of("articleId", articleId, "userId", userId));
    }

    //뉴스기사 댓글수
    Long commentCount = commentRepository.countByArticleIdAndIsDeletedFalse(articleId);

    //기사 조회수 증가
    // ToDo: 조회수 증가 로직의 동시성 문제를 DB 레벨에서 처리
    // ToDo: DB 레벨 원자적 증가/낙관적 락/ 비관적 락 고려 -> DB레벨 원자적 증가 선택
    newsArticleRepository.incrementViewCount(articleId);

    return newsArticleViewMapper.toDto(savedArticleView, commentCount);

  }

  @Transactional(readOnly = true)
  public CursorPageResponseNewsArticleDto list(NewsArticlePageRequest request, UUID userId){

    //요청 유저가 존재하는지 확인하고 삭제되지 않은 활성 사용자인지 검증
    validateActiveUser(userId);

    //요청 조건에 맞는 뉴스기사 목록을 limit + 1 개수만큼 조회
    //limit + 1만큼 조회하는 이유는 다음 페이지 존재여부(hasNext) 판단을 위해서
    List<NewsArticleCursorRow> rows = new ArrayList<>(newsArticleRepository.findNewsArticles(request, userId));

    //조회된 개수가 요청 limit을 초과하면 다음 페이지가 존재한다고 판단
    boolean hasNext = rows.size() > request.limit();

    //다음 페이지가 존재하는 경우 반환 데이터는 limit 개수만큼 잘라내기
    //limit + 1로 가져온 마지막 1개는 hasNext 판단용이라서 제거
    if(hasNext){
      rows = new ArrayList<>(rows.subList(0, request.limit()));
    }

    //내부 조회용 DTO(NewsArticleCursorRow)를 외부 응답용 DTO(NewsArticleDto)로 변환
    // cretaedAt 내부 필드는 제외
    List<NewsArticleDto> content = rows.stream()
        .map(NewsArticleCursorRow::toDto)
        .toList();

    //다음 페이지를 위한 커서값 초기화
    String nextCursor = null;
    String nextAfter = null;

    //다음 페이지 존재하고, 현재 페이지 데이터 비어있지 않은 경우에만 커서 계산
    if(hasNext && !rows.isEmpty()){

      //현재 페이지의 마지막 데이터를 기준으로 다음 페이지 커서를 생성
      NewsArticleCursorRow last = rows.get(rows.size() - 1);

      //정렬 기준(orderBy)에 따라 다음 페이지를 위한 cursor 값 설정
      //주커서: 정렬 기준 필드 값
      String cursor = switch(request.orderBy()){
        case publishDate -> last.publishDate().toString();
        case commentCount -> String.valueOf(last.commentCount());
        case viewCount -> String.valueOf(last.viewCount());
      };

      nextAfter = last.createdAt().toString();

      // cursor 안에 정렬값 + createdAt 포함
      // 클라이언트에서 after를 안보내줘서 cursor에 cursor + after값을 넣었다.
      nextCursor = cursor + "|" + nextAfter;
    }

    //전체 데이터 개수 조회
    long totalElements = newsArticleRepository.countNewsArticles(request);

    return new CursorPageResponseNewsArticleDto(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        totalElements,
        hasNext
    );

  }
  @Transactional(readOnly = true)
  public NewsArticleDto findById(UUID articleId, UUID userId) {

    // 요청 유저가 존재하는지 확인하고 삭제되지 않은 활성 사용자인지 검증
    validateActiveUser(userId);

    //1. 기사 조회
    NewsArticle article = getNewsArticle(articleId);

    //2. 논리 삭제 체크
    if (article.isDeleted()) {
      throw new ArticleException(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED,
          Map.of("articleId", articleId));
    }

    // 3. 댓글 수 조회
    Long commentCount = commentRepository.countByArticleIdAndIsDeletedFalse(articleId);

    // 4. 조회했던 기사인지 여부
    Boolean viewedByMe = articleViewRepository.existsByNewsArticleIdAndUserId(articleId, userId);

    return newsArticleMapper.toDto(article, commentCount, viewedByMe);

  }

  @Transactional(readOnly = true)
  public List<ArticleSource> findAllSources() {
    return List.of(ArticleSource.values());
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

  private void validateActiveUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND,
            Map.of("userId", userId)));

    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
  }
}
