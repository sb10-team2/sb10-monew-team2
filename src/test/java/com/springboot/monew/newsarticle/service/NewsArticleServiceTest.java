package com.springboot.monew.newsarticle.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.newsarticle.dto.CollectedArticleWithInterest;
import com.springboot.monew.newsarticle.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticle.dto.response.CollectedArticle;
import com.springboot.monew.newsarticle.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticle.dto.response.NewsArticleCursorRow;
import com.springboot.monew.newsarticle.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticle.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticle.entity.ArticleInterest;
import com.springboot.monew.newsarticle.entity.ArticleView;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import com.springboot.monew.newsarticle.enums.ArticleSource;
import com.springboot.monew.newsarticle.enums.NewsArticleDirection;
import com.springboot.monew.newsarticle.enums.NewsArticleOrderBy;
import com.springboot.monew.newsarticle.exception.ArticleException;
import com.springboot.monew.newsarticle.exception.NewsArticleErrorCode;
import com.springboot.monew.newsarticle.mapper.NewsArticleMapper;
import com.springboot.monew.newsarticle.mapper.NewsArticleViewMapper;
import com.springboot.monew.newsarticle.repository.ArticleInterestRepository;
import com.springboot.monew.newsarticle.repository.ArticleViewRepository;
import com.springboot.monew.newsarticle.repository.NewsArticleRepository;
import com.springboot.monew.notification.event.InterestNotificationEvent;
import com.springboot.monew.user.document.UserActivityDocument.ArticleViewItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.event.articleView.ArticleViewedEvent;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserActivityOutboxService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

//Mockito를 사용하기위한 어노테이션
//UserRepository같은 가짜 객체 자동 생성(Mock), Mock을 UserService에 주입(@InjectMocks)를 위한 어노테이션
@ExtendWith(MockitoExtension.class)
class NewsArticleServiceTest {

  @Mock
  private NewsArticleRepository newsArticleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private ArticleViewRepository articleViewRepository;

  @Mock
  private NewsArticleMapper newsArticleMapper;

  @Mock
  private NewsArticleViewMapper newsArticleViewMapper;

  @Mock
  private ArticleInterestRepository articleInterestRepository;

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private UserActivityOutboxService userActivityOutboxService;

  @InjectMocks
  private NewsArticleService newsArticleService;

  //1. DB에 없는 신규기사 1개가 들어왔을때 기사저장
  //2. 기사-관심사 연결 저장
  //3. 알림 이벤트 발행
  @Test
  @DisplayName("신규기사와 관심사 연결 저장 saveAll 성공")
  void saveAll_success_saveNewArticlesAndArticleinterests() {

    //given
    UUID interestId = UUID.randomUUID();

    //수집된 기사 DTO 생성
    CollectedArticle collectedArticle = mock(CollectedArticle.class);
    given(collectedArticle.originalLink()).willReturn("https://test.com/1");

    //수집기사와 관심사 ID연결 DTO생성
    //(article, Set<UUID> interestIds)
    CollectedArticleWithInterest item = new CollectedArticleWithInterest(collectedArticle, Set.of(interestId));

    //저장 전 신규 기사 엔티티 생성
    NewsArticle newsArticle = mock(NewsArticle.class);

    //저장 후 기사 Entity 생성
    NewsArticle savedArticle = mock(NewsArticle.class);

    //관심사 참조 엔티티 생성
    Interest interest = mock(Interest.class);

    //DB에 이미 존재하는 기사 조회 -> 없음
    given(newsArticleRepository.findAllByOriginalLinkIn(Set.of("https://test.com/1"))).willReturn(List.of());

    //수집 기사 DTO를 NewsArticle 엔티티로 변환
    //DB에 없는 신규기사를 엔티티로 변환
    given(newsArticleMapper.toEntity(collectedArticle)).willReturn(newsArticle);

    //신규 기사 저장 결과 설정
    given(newsArticleRepository.saveAll(List.of(newsArticle))).willReturn(List.of(savedArticle));

    //저장된 기사 originalLink 설정
    given(savedArticle.getOriginalLink()).willReturn("https://test.com/1");

    //저장된 기사 ID 설정
    given(savedArticle.getId()).willReturn(UUID.randomUUID());

    //기존 article_interest 연결없음.
    given(articleInterestRepository.findAllByNewsArticleIn(List.of(savedArticle))).willReturn(List.of());

    //관심사 프로시 조회 결과 설정
    given(interestRepository.getReferenceById(interestId)).willReturn(interest);

    //  when
    newsArticleService.saveAll(List.of(item));

    //  then
    //originalLink기준으로 기존 기사 조회가 수행되어야한다.
    verify(newsArticleRepository).findAllByOriginalLinkIn(Set.of("https://test.com/1"));

    //신규 기사 변환이 수행되어야한다.
    verify(newsArticleMapper).toEntity(collectedArticle);

    //신규 기사 저장이 수행되어야한다.
    verify(newsArticleRepository).saveAll(List.of(newsArticle));

    //기존 기사-관심사 연결 조회가 수행되어야한다.
    verify(articleInterestRepository).findAllByNewsArticleIn(List.of(savedArticle));

    //관심사 참조 조회가 수행되어야한다.
    verify(interestRepository).getReferenceById(interestId);

    // 신규 article_interest 저장이 수행되어야 한다
    verify(articleInterestRepository).saveAll(anyList());

    // 관심사 알림 이벤트가 발행되어야 한다
    verify(eventPublisher).publishEvent(any(InterestNotificationEvent.class));
  }

  @Test
  @DisplayName("수집된 CollectedArticleWithInterest가 빈목록이면 아무 작업하지 않는다.")
  void saveAll_success_doNothing_whenEmpty() {
    //  given
    List<CollectedArticleWithInterest> emptyItems = List.of();

    //  when
    newsArticleService.saveAll(emptyItems);

    //  then

    //입력이 비어있으면 기존 기사 조회가 수행되면 안된다.
    verify(newsArticleRepository, never()).findAllByOriginalLinkIn(any());

    //입력이 비어있으면 기사 저장이 수행되면 안된다.
    verify(newsArticleRepository, never()).saveAll(anyList());

    //입력이 비어있으면 기사-관심사 저장이 수행되면 안된다.
    verify(articleInterestRepository, never()).saveAll(anyList());

    //이벤트도 발행되면 안된다.
    verify(eventPublisher, never()).publishEvent(any());
  }

  //List<CollectedArticleWithInterest> articlesWithInterests이 (뉴스A, 관심사1)이렇게 구성돼 있다 치자.
  //뉴스A의 originalLink가 " "로 들어왔을때 distinctItems가 빈값이 반환되고, return 되는 테스트이다.
  @Test
  @DisplayName("originalLink가 없으면 아무작업을 하지 않는다.")
  void saveAll_success_doNothing_whenNotFoundOriginalLink() {
    //  given
    // originalLink가 blank인 수집 기사 생성
    CollectedArticle collectedArticle = mock(CollectedArticle.class);
    given(collectedArticle.originalLink()).willReturn(" ");

    CollectedArticleWithInterest item = new CollectedArticleWithInterest(collectedArticle, Set.of(UUID.randomUUID()));

    //  when
    newsArticleService.saveAll(List.of(item));

    //  then
    //유효한 originalLink가 없으면 기존 기사 조회가 수행되면 안된다.
    verify(newsArticleRepository, never()).findAllByOriginalLinkIn(any());

    //신규 기사 저장도 수행되면 안된다.
    verify(newsArticleRepository, never()).saveAll(anyList());

    //기사-관심사 저장도 수행되면 안된다.
    verify(articleInterestRepository, never()).saveAll(anyList());
  }

  //(뉴스A, 관심사1),(뉴스A, 관심사2),(뉴스B, 관심사3)
  //뉴스A.originalLink -> (관심사1,관심사2)
  //뉴스B.originalLink -> (관심사3)
  //이렇게 구성하는 Test
  @Test
  @DisplayName("중복 originalLink는 하나의 기사로 저장하고 관심사ID를 합친다.")
  void saveAll_success_mergeInterestIds_whenOriginalLinkDuplicated() {
    //  given
    UUID interestId1 = UUID.randomUUID();
    UUID interestId2 = UUID.randomUUID();

    //같은 originalLink를 가진 수집 기사 2개 생성
    CollectedArticle article1 = mock(CollectedArticle.class);
    CollectedArticle article2 = mock(CollectedArticle.class);
    given(article1.originalLink()).willReturn("https://test.com/1");
    given(article2.originalLink()).willReturn("https://test.com/1");

    CollectedArticleWithInterest item1 = new CollectedArticleWithInterest(article1, Set.of(interestId1));
    CollectedArticleWithInterest item2 = new CollectedArticleWithInterest(article2, Set.of(interestId2));

    NewsArticle newsArticle = mock(NewsArticle.class);
    NewsArticle savedArticle = mock(NewsArticle.class);

    Interest interest1 = mock(Interest.class);
    Interest interest2 = mock(Interest.class);

    UUID savedArticleId = UUID.randomUUID();

    //DB에 기존 기사 없음
    given(newsArticleRepository.findAllByOriginalLinkIn(Set.of("https://test.com/1")))
        .willReturn(List.of());

    // 중복 제거 후 대표 기사만 엔티티로 변환됨
    given(newsArticleMapper.toEntity(article1)).willReturn(newsArticle);

    // 신규 기사 저장
    given(newsArticleRepository.saveAll(List.of(newsArticle))).willReturn(List.of(savedArticle));

    // 저장된 기사 정보 설정
    given(savedArticle.getOriginalLink()).willReturn("https://test.com/1");
    given(savedArticle.getId()).willReturn(savedArticleId);

    // 기존 연결 없음
    given(articleInterestRepository.findAllByNewsArticleIn(List.of(savedArticle)))
        .willReturn(List.of());

    // 두 관심사 참조 조회
    given(interestRepository.getReferenceById(interestId1)).willReturn(interest1);
    given(interestRepository.getReferenceById(interestId2)).willReturn(interest2);

    //  when
    newsArticleService.saveAll(List.of(item1, item2));

    // then
    // 같은 originalLink는 한 번만 저장되어야 한다
    verify(newsArticleRepository).saveAll(List.of(newsArticle));

    // article1만 대표로 변환되고 article2는 별도 기사로 변환되지 않아야 한다
    verify(newsArticleMapper).toEntity(article1);
    verify(newsArticleMapper, never()).toEntity(article2);

    // 합쳐진 관심사 ID 2개에 대해 참조 조회가 수행되어야 한다
    verify(interestRepository).getReferenceById(interestId1);
    verify(interestRepository).getReferenceById(interestId2);

    // article_interest 저장이 수행되어야 한다
    // 저장된 list의 크기는 2
    verify(articleInterestRepository).saveAll(argThat((List<ArticleInterest> list) -> list.size() == 2));

    // 이벤트가 발행되어야 한다
    // List<InterestNotificationEvent> 타입 객체
    verify(eventPublisher, times(2)).publishEvent(any(InterestNotificationEvent.class));

  }

  @Test
  @DisplayName("기존에 기사-관심사연결이 이미 있으면 중복 저장하지 않는다.")
  void saveAll_success_skipDuplicateArticleInterest_whenRelationAlreadyExists() {
    // given
    UUID interestId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    CollectedArticle collectedArticle = mock(CollectedArticle.class);
    given(collectedArticle.originalLink()).willReturn("https://test.com/1");

    CollectedArticleWithInterest item =
        new CollectedArticleWithInterest(collectedArticle, Set.of(interestId));

    NewsArticle existingArticle = mock(NewsArticle.class);
    Interest existingInterest = mock(Interest.class);
    ArticleInterest existingArticleInterest = mock(ArticleInterest.class);

    // 기존 기사 정보 설정
    given(existingArticle.getOriginalLink()).willReturn("https://test.com/1");
    given(existingArticle.getId()).willReturn(articleId);

    // 기존 관심사 정보 설정
    given(existingInterest.getId()).willReturn(interestId);

    // 기존 article_interest 연결 정보 설정
    given(existingArticleInterest.getNewsArticle()).willReturn(existingArticle);
    given(existingArticleInterest.getInterest()).willReturn(existingInterest);

    // DB에 이미 기사 존재
    given(newsArticleRepository.findAllByOriginalLinkIn(Set.of("https://test.com/1")))
        .willReturn(List.of(existingArticle));

    // 이미 기사-관심사 연결도 존재
    given(articleInterestRepository.findAllByNewsArticleIn(List.of(existingArticle)))
        .willReturn(List.of(existingArticleInterest));

    // when
    newsArticleService.saveAll(List.of(item));

    // then
    // 기존 기사이므로 신규 기사 저장은 수행되면 안 된다
    verify(newsArticleRepository, never()).saveAll(anyList());

    // 이미 연결이 있으므로 관심사 참조 조회가 수행되면 안 된다
    verify(interestRepository, never()).getReferenceById(any());

    // 이미 연결이 있으므로 article_interest 저장도 수행되면 안 된다
    verify(articleInterestRepository, never()).saveAll(anyList());

    // 신규 연결이 없으므로 이벤트도 발행되면 안 된다
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("뉴스기사 조회이력 등록에 성공한다.")
  void createView_success_normal() {
    //  given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    // 저장된 ArticleView의 id를 따로 고정해둔다.
    UUID articleViewId = UUID.randomUUID();
    User user = mock(User.class);
    NewsArticle newsArticle = mock(NewsArticle.class);

    //저장된 조회이력 mock 생성
    //ArticleView savedArticleView;
    ArticleView savedArticleView = mock(ArticleView.class);

    // saveAndFlush 이후 다시 조회해 사용할 ArticleView mock
    ArticleView refreshedArticleView = mock(ArticleView.class);

    //반환될 DTO mock 생성
    NewsArticleViewDto responseDto = mock(NewsArticleViewDto.class);

    // Outbox 저장 시 전달될 ArticleViewItem을 미리 준비한다.
    ArticleViewItem articleViewItem = new ArticleViewItem(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        articleId,
        ArticleSource.NAVER,
        "https://example.com/article",
        "기사 제목",
        Instant.parse("2026-04-28T00:00:00Z"),
        "기사 요약",
        3L,
        11L
    );

    //validateActiveUser(userId)로 사용자 존재
    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    //getNewsArticle(articleId)로 뉴스기사 존재
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(newsArticle));

    //newsArticle.idsDelete()로 논리삭제 안된 뉴스기사 존재
    given(newsArticle.isDeleted()).willReturn(false);

    //아직 조회하지 않은 기사로 설정
    given(articleViewRepository.existsByNewsArticleIdAndUserId(articleId, userId)).willReturn(false);

    //조회이력 저장 성공
    given(articleViewRepository.saveAndFlush(any(ArticleView.class))).willReturn(savedArticleView);

    // 저장된 조회이력의 id를 반환하도록 설정
    given(savedArticleView.getId()).willReturn(articleViewId);

    // getReferenceById()로 다시 조회한 ArticleView mock 반환
    given(articleViewRepository.getReferenceById(articleViewId)).willReturn(refreshedArticleView);

    //뉴스기사 댓글 수
    given(commentRepository.countByArticleIdAndIsDeletedFalse(articleId)).willReturn(3L);

    // DTO 변환은 refreshedArticleView 기준으로 stub
    given(newsArticleViewMapper.toDto(eq(refreshedArticleView), eq(3L))).willReturn(responseDto);

    // Outbox/Event용 ArticleViewItem도 refreshedArticleView 기준으로 stub
    given(newsArticleViewMapper.toArticleViewItem(eq(refreshedArticleView), eq(3L)))
        .willReturn(articleViewItem);

    //  when
    NewsArticleViewDto result = newsArticleService.createView(articleId, userId);

    //  then
    //반환 DTO 검증
    assertThat(result).isEqualTo(responseDto);
    verify(newsArticleRepository).findById(articleId);
    verify(articleViewRepository).existsByNewsArticleIdAndUserId(articleId, userId);
    verify(articleViewRepository).saveAndFlush(any(ArticleView.class));
    verify(savedArticleView).getId();
    verify(articleViewRepository).getReferenceById(articleViewId);
    verify(commentRepository).countByArticleIdAndIsDeletedFalse(articleId);

    //조회수 증가가 수행되었는지 검증
    verify(newsArticleRepository).incrementViewCount(articleId);

    //DTO 변환이 수행되었는지 검증
    verify(newsArticleViewMapper).toDto(any(ArticleView.class), eq(3L));
    verify(newsArticleViewMapper).toArticleViewItem(eq(refreshedArticleView), eq(3L));

    // 기사 조회 성공 시 사용자 활동내역 반영을 위한 ArticleViewedEvent가 발행되었는지 검증한다.
    ArgumentCaptor<ArticleViewedEvent> captor =
        ArgumentCaptor.forClass(ArticleViewedEvent.class);

    verify(eventPublisher).publishEvent(captor.capture());

    // 발행된 이벤트에 조회한 사용자 id와 기사 조회 활동 정보가 올바르게 담겼는지 검증한다.
    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().item()).isEqualTo(articleViewItem);

    // 기사 조회 사용자의 활동 문서 반영용 Outbox 저장이 호출되었는지 검증한다.
    verify(userActivityOutboxService).saveArticleViewed(userId, articleViewItem);
  }

  @Test
  @DisplayName("존재하지 않는 뉴스기사 조회 이력 등록시 실패한다.")
  void createView_fail_articleNotFound() {
    //  given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.empty());

    //  when, then
    assertThatThrownBy(() -> newsArticleService.createView(articleId, userId))
        .isInstanceOf(ArticleException.class)
        .satisfies(throwable -> {
          ArticleException exception = (ArticleException) throwable;

          //에러코드 검증
          assertThat(exception.getErrorCode()).isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND);

          //details 검증
          assertThat(exception.getDetails()).isEqualTo(Map.of("articleId", articleId));
        });

    //사용자조회, 뉴스기사 조회까지만 수행
    verify(userRepository).findById(userId);
    verify(newsArticleRepository).findById(articleId);

    //뉴스기사 뷰 save와 조회수 증가는 실행되면 안된다.
    verify(articleViewRepository, never()).saveAndFlush(any(ArticleView.class));
    verify(newsArticleRepository, never()).incrementViewCount(articleId);
  }

  @Test
  @DisplayName("논리삭제된 뉴스기사 조회 이력 등록 시 실패한다")
  void createView_fail_articleAlreadyDeleted() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = mock(User.class);
    NewsArticle newsArticle = mock(NewsArticle.class);

    // 사용자 조회 성공
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    // 뉴스기사 조회 성공
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(newsArticle));

    // 뉴스기사가 논리삭제된 상태
    given(newsArticle.isDeleted()).willReturn(true);

    // when & then
    assertThatThrownBy(() -> newsArticleService.createView(articleId, userId))
        .isInstanceOf(ArticleException.class)
        .satisfies(throwable -> {
          ArticleException exception = (ArticleException) throwable;

          assertThat(exception.getErrorCode())
              .isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED);

          assertThat(exception.getDetails())
              .isEqualTo(Map.of("articleId", articleId));
        });

    // 조회 이력 중복 확인은 수행되면 안 됨
    verify(articleViewRepository, never()).existsByNewsArticleIdAndUserId(any(), any());

    // 조회 이력 저장도 수행되면 안 됨
    verify(articleViewRepository, never()).saveAndFlush(any());

    // 조회수 증가도 수행되면 안 됨
    verify(newsArticleRepository, never()).incrementViewCount(any());
  }

  @Test
  @DisplayName("이미 조회한 뉴스기사 조회 이력 등록 시 실패한다")
  void createView_fail_alreadyViewed() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = mock(User.class);
    NewsArticle newsArticle = mock(NewsArticle.class);

    // 사용자 조회 성공
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    // 뉴스기사 조회 성공
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(newsArticle));
    given(newsArticle.isDeleted()).willReturn(false);

    // 이미 조회한 기사로 설정
    given(articleViewRepository.existsByNewsArticleIdAndUserId(articleId, userId))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> newsArticleService.createView(articleId, userId))
        .isInstanceOf(ArticleException.class)
        .satisfies(throwable -> {
          ArticleException exception = (ArticleException) throwable;

          assertThat(exception.getErrorCode())
              .isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_VIEWED);

          assertThat(exception.getDetails())
              .isEqualTo(Map.of("articleId", articleId, "userId", userId));
        });

    // 조회 이력 저장은 수행되면 안 됨
    verify(articleViewRepository, never()).saveAndFlush(any());

    // 조회수 증가도 수행되면 안 됨
    verify(newsArticleRepository, never()).incrementViewCount(any());
  }



  @Test
  @DisplayName("뉴스기사 목록 조회시 다음 페이지가 있으면 nextCursor에 정렬값과 nextAfter를 포함한다.")
  void list_success_hasNext() {

    //  given
    UUID userId = UUID.randomUUID();

    //요청 조건 생성
    //다음 페이지가 있는지만 확인하면 되므로 요청값은 null로 처리
    //임의로 정렬기준 viewCount로 설정
    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.viewCount,
        NewsArticleDirection.DESC,
        null,
        null,
        2
    );

    //활성 사용자 mock 생성
    //validateActiveUser()
    User user = mock(User.class);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    //limit + 1개의 조회 결과 생성
    //request.limit이 2라서 3개 생성
    NewsArticleCursorRow row1 = new NewsArticleCursorRow(
        UUID.randomUUID(),
        ArticleSource.NAVER,
        "https://test.com/1",
        "title1",
        Instant.parse("2026-04-24T10:00:00Z"),
        "summary1",
        3L,
        100L,
        false,
        Instant.parse("2026-04-24T09:00:00Z")
    );

    NewsArticleCursorRow row2 = new NewsArticleCursorRow(
        UUID.randomUUID(),
        ArticleSource.NAVER,
        "https://test.com/2",
        "title2",
        Instant.parse("2026-04-24T09:00:00Z"),
        "summary2",
        2L,
        90L,
        false,
        Instant.parse("2026-04-24T08:00:00Z")
    );

    NewsArticleCursorRow row3 = new NewsArticleCursorRow(
        UUID.randomUUID(),
        ArticleSource.NAVER,
        "https://test.com/3",
        "title3",
        Instant.parse("2026-04-24T08:00:00Z"),
        "summary3",
        1L,
        80L,
        false,
        Instant.parse("2026-04-24T07:00:00Z")
    );

    //Repository가 limit + 1개를 반환하도록 설정
    //newsArticleRepository.findNewsArticles(request, userId)
    given(newsArticleRepository.findNewsArticles(request, userId)).willReturn(List.of(row1, row2, row3));

    //전체 기사 개수 반환 설정(totalElements)
    //newsArticleRepository.countNewsArticles(request);
    given(newsArticleRepository.countNewsArticles(request)).willReturn(3L);

    //  when
    CursorPageResponseNewsArticleDto response = newsArticleService.list(request, userId);

    //  then
    // limit이 2개, 조회결과가 3개라서 hasNext가 true여야한다.
    assertThat(response.hasNext()).isTrue();

    //응답 content는 limit(2)만큼 잘려있어야한다.
    assertThat(response.content()).hasSize(2);

    //마지막으로 반환된 row2기준으로 nextCursor가 생성되어야한다.
    assertThat(response.nextCursor()).isEqualTo("90|2026-04-24T08:00:00Z");

    //nextAfter는 마지막 반환 데이터의 createdAt이어야한다.
    assertThat(response.nextAfter()).isEqualTo("2026-04-24T08:00:00Z");

    //뉴스기사 전체 개수는 Repository count 결과와 같아야한다.
    assertThat(response.totalElements()).isEqualTo(3L);

    //사용자 검증이 수행되어야한다.
    verify(userRepository).findById(userId);

    // 기사 목록 조회가 수행되어야 한다
    verify(newsArticleRepository).findNewsArticles(request, userId);

    // 전체 기사 개수 조회가 수행되어야 한다
    verify(newsArticleRepository).countNewsArticles(request);

  }


  @Test
  @DisplayName("뉴스기사 목록 조회시 다음 페이지가 없으면 nextCursor와 nextAfter에 null값이 들어간다")
  void list_success_noHasNext() {

    //  given
    UUID userId = UUID.randomUUID();

    //요청 조건 생성
    //다음 페이지가 있는지만 확인하면 되므로 요청값은 null로 처리
    //임의로 정렬기준 viewCount로 설정
    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.viewCount,
        NewsArticleDirection.DESC,
        null,
        null,
        2
    );

    //활성 사용자 mock 생성
    //validateActiveUser()
    User user = mock(User.class);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    //limit + 1개의 조회 결과 생성
    //request.limit이 2라서 3개 생성
    NewsArticleCursorRow row1 = new NewsArticleCursorRow(
        UUID.randomUUID(),
        ArticleSource.NAVER,
        "https://test.com/1",
        "title1",
        Instant.parse("2026-04-24T10:00:00Z"),
        "summary1",
        3L,
        100L,
        false,
        Instant.parse("2026-04-24T09:00:00Z")
    );

    NewsArticleCursorRow row2 = new NewsArticleCursorRow(
        UUID.randomUUID(),
        ArticleSource.NAVER,
        "https://test.com/2",
        "title2",
        Instant.parse("2026-04-24T09:00:00Z"),
        "summary2",
        2L,
        90L,
        false,
        Instant.parse("2026-04-24T08:00:00Z")
    );

    //Repository가 limit + 1개를 반환하도록 설정
    //newsArticleRepository.findNewsArticles(request, userId)
    given(newsArticleRepository.findNewsArticles(request, userId)).willReturn(List.of(row1, row2));

    //전체 기사 개수 반환 설정(totalElements)
    //newsArticleRepository.countNewsArticles(request);
    given(newsArticleRepository.countNewsArticles(request)).willReturn(2L);

    //  when
    CursorPageResponseNewsArticleDto response = newsArticleService.list(request, userId);

    //  then
    // limit이 2개, 조회결과가 2개라서 hasNext가 false
    assertThat(response.hasNext()).isFalse();

    //응답 content는 limit(2)만큼 잘려있어야한다.
    assertThat(response.content()).hasSize(2);

    //hasNext가 false가서 nextCursor는 null이여야한다.
    assertThat(response.nextCursor()).isNull();

    //hasNext가 false가서 nextAfter는 null이여야한다.
    assertThat(response.nextAfter()).isNull();

    //뉴스기사 전체 개수는 Repository count 결과와 같아야한다.
    assertThat(response.totalElements()).isEqualTo(2L);

    //사용자 검증이 수행되어야한다.
    verify(userRepository).findById(userId);

    // 기사 목록 조회가 수행되어야 한다
    verify(newsArticleRepository).findNewsArticles(request, userId);

    // 전체 기사 개수 조회가 수행되어야 한다
    verify(newsArticleRepository).countNewsArticles(request);

  }

  @Test
  @DisplayName("존재하지 않는 사용자로 뉴스기사 목록조회시 실패한다.")
  void list_fail_userNotFound() {
    //  given
    UUID userId = UUID.randomUUID();

    //요청 조건 생성
    //다음 페이지가 있는지만 확인하면 되므로 요청값은 null로 처리
    //임의로 정렬기준 viewCount로 설정
    NewsArticlePageRequest request = new NewsArticlePageRequest(
        null,
        null,
        null,
        null,
        null,
        NewsArticleOrderBy.viewCount,
        NewsArticleDirection.DESC,
        null,
        null,
        2
    );

    //존재하지 않은 사용자 준비
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    //  when, then
    assertThatThrownBy(() -> newsArticleService.list(request, userId))
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {

          //발생한 예외를 UserExcepton으로 변환
          UserException exception = (UserException) throwable;

          //에러코드가 USER_NOT_FOUND인지 검증
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);

          //details에 userId가 포함돼있는지 검증
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    //사용자 조회는 수행 되어야한다.
    verify(userRepository).findById(userId);

    //사용자 조회에서 실패했으므로, 기사조회는 수행되면 안된다.
    verify(newsArticleRepository, never()).findNewsArticles(request, userId);

    //사용자 조회에서 실패했으므로, count조회도 수행되면 안된다.
    verify(newsArticleRepository, never()).countNewsArticles(request);
  }

  @Test
  @DisplayName("articleId에 해당하는 뉴스기사가 있을때 단건조회에 성공한다.")
  void findById_success_normal() {

    //  given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    //사용자,기사 mock 생성
    User user = mock(User.class);
    NewsArticle article = mock(NewsArticle.class);

    //반환될 DTO mock 생성
    NewsArticleDto articleDto = mock(NewsArticleDto.class);

    //사용자 유효성 검증 성공
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    //뉴스기사 조회
    //getNewsArticle()
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));

    //댓글 수 2개, 이미 조회했던 기사 true로 임의설정
    given(commentRepository.countByArticleIdAndIsDeletedFalse(articleId)).willReturn(2L);
    given(articleViewRepository.existsByNewsArticleIdAndUserId(articleId, userId)).willReturn(true);

    //DTO로 변환
    given(newsArticleMapper.toDto(article, 2L, true)).willReturn(articleDto);

    //  when
    NewsArticleDto response = newsArticleService.findById(articleId, userId);

    // then
    assertThat(response).isEqualTo(articleDto);
    verify(userRepository).findById(userId);
    verify(newsArticleRepository).findById(articleId);
    verify(commentRepository).countByArticleIdAndIsDeletedFalse(articleId);
    verify(articleViewRepository).existsByNewsArticleIdAndUserId(articleId, userId);
    verify(newsArticleMapper).toDto(article, 2L, true);
  }

  @Test
  @DisplayName("articleId에 해당하는 뉴스기사가 없을경우 단건조회에 실패한다.")
  void findById_fail_whenArticleNotFound() {

    //  given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    //사용자 조회 성공
    User user = mock(User.class);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    //기사 조회 실패
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.empty());

    //  when, then
    assertThatThrownBy(() -> newsArticleService.findById(articleId, userId))
        .isInstanceOf(ArticleException.class)
        .satisfies(throwable -> {
          ArticleException exception = (ArticleException) throwable;

          // 에러 코드 검증
          assertThat(exception.getErrorCode())
              .isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND);

          // details 검증
          assertThat(exception.getDetails())
              .isEqualTo(Map.of("articleId", articleId));
        });

    // 기사 조회까지만 수행되고 이후 로직은 실행되지 않아야 함
    verify(newsArticleRepository).findById(articleId);
    verify(commentRepository, never()).countByArticleIdAndIsDeletedFalse(any());
    verify(articleViewRepository, never()).existsByNewsArticleIdAndUserId(any(), any());

  }

  @Test
  @DisplayName("articleId에 해당하는 뉴스기사가 논리삭제된 경우 단건조회에 실패해야한다.")
  void findById_fail_deletedArticle() {
    // given

    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = mock(User.class);
    NewsArticle article = mock(NewsArticle.class);

    // 사용자 정상
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    // 기사 존재
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));

    // 논리 삭제된 기사
    given(article.isDeleted()).willReturn(true);

    // when & then

    assertThatThrownBy(() -> newsArticleService.findById(articleId, userId))
        .isInstanceOf(ArticleException.class)
        .satisfies(throwable -> {
          ArticleException exception = (ArticleException) throwable;

          assertThat(exception.getErrorCode())
              .isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED);

          assertThat(exception.getDetails())
              .isEqualTo(Map.of("articleId", articleId));
        });

    // 댓글 조회는 수행되면 안 됨
    verify(commentRepository, never()).countByArticleIdAndIsDeletedFalse(any());
  }

  @Test
  @DisplayName("articleId에 해당하는 뉴스기사가 있을경우 물리삭제에 성공해야한다.")
  void hardDelete_success_existArticle() {
    //  given
    UUID articleId = UUID.randomUUID();
    NewsArticle article = mock(NewsArticle.class);

    //articleId에 해당하는 뉴스기사가 존재한다.
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));

    // when
    newsArticleService.hardDelete(articleId);

    // then
    verify(newsArticleRepository).findById(articleId);
    verify(newsArticleRepository).delete(article);

  }

  @Test
  @DisplayName("articleId에 해당하는 뉴스기사가 없을경우 물리삭제에 실패해야한다.")
  void hardDelete_fail_not_existArticle() {
    //  given
    UUID articleId = UUID.randomUUID();
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.empty());

    //  when, then
    assertThatThrownBy(() -> newsArticleService.hardDelete(articleId))
        .isInstanceOf(ArticleException.class)
        .satisfies(throwable -> {

          //발생한 예외를 ArticleException으로 변환
          ArticleException exception = (ArticleException) throwable;

          //에러코드가 NEWS_ARTICLE_NOT_FOUND인지 검증
          assertThat(exception.getErrorCode()).isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND);

          //details에 articleId가 포함돼있는지 검증
          assertThat(exception.getDetails()).isEqualTo(Map.of("articleId", articleId));
        });

    //articleId로 뉴스기사 조회했는지 검증
    verify(newsArticleRepository).findById(articleId);

    //뉴스기사 없으므로 delete()가 호출되지 않았는지 검증
    verify(newsArticleRepository, never()).delete(any(NewsArticle.class));
  }

  @Test
  @DisplayName("articleId에 해당하는 뉴스기사가 있을경우 논리삭제에 성공해야한다.")
  void softDelete_success_existArticle() {
    //  given
    UUID articleId = UUID.randomUUID();
    NewsArticle article = mock(NewsArticle.class);

    //articleId에 해당하는 뉴스기사가 존재한다.
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));

    //논리삭제가 안된 뉴스기사이다.
    given(article.isDeleted()).willReturn(false);

    //  when
    newsArticleService.softDelete(articleId);

    //then
    verify(newsArticleRepository).findById(articleId);
    verify(article).isDeleted();
    verify(article).delete();

    //논리삭제이므로, repository.delete()는 호출되지 않아야한다.
    verify(newsArticleRepository, never()).delete(any(NewsArticle.class));
  }

  @Test
  @DisplayName("논리삭제된 뉴스기사를 논리삭제할경우, 논리삭제에 실패해야한다.")
  void softDelete_fail_newsArticle_already_deleted(){
    //  given
    UUID articleId = UUID.randomUUID();
    NewsArticle article = mock(NewsArticle.class);
    given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(article.isDeleted()).willReturn(true);
    //  when, then
    assertThatThrownBy(() -> newsArticleService.softDelete(articleId))
        .isInstanceOf(ArticleException.class)
        .satisfies(throwable -> {
          ArticleException exception = (ArticleException) throwable;

          //에러코드가 NEWS_ARTICLE_ALREADY_DELETED가 맞는지 검증
          assertThat(exception.getErrorCode()).isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED);

          assertThat(exception.getDetails()).isEqualTo(Map.of("articleId", articleId));

        });

    verify(newsArticleRepository).findById(articleId);
    verify(article).isDeleted();
    verify(article, never()).delete();
  }
}