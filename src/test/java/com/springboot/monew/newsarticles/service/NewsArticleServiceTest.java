package com.springboot.monew.newsarticles.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.newsarticles.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticles.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleCursorRow;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.entity.ArticleView;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.enums.NewsArticleDirection;
import com.springboot.monew.newsarticles.enums.NewsArticleOrderBy;
import com.springboot.monew.newsarticles.exception.ArticleException;
import com.springboot.monew.newsarticles.exception.NewsArticleErrorCode;
import com.springboot.monew.newsarticles.mapper.NewsArticleMapper;
import com.springboot.monew.newsarticles.mapper.NewsArticleViewMapper;
import com.springboot.monew.newsarticles.repository.ArticleViewRepository;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @InjectMocks
  private NewsArticleService newsArticleService;

  @Test
  @DisplayName("뉴스기사 조회이력 등록에 성공한다.")
  void createView_success_normal() {
    //  given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    NewsArticle newsArticle = mock(NewsArticle.class);

    //저장된 조회이력 mock 생성
    //ArticleView savedArticleView;
    ArticleView savedArticleView = mock(ArticleView.class);

    //반환될 DTO mock 생성
    NewsArticleViewDto responseDto = mock(NewsArticleViewDto.class);

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

    //뉴스기사 댓글 수
    given(commentRepository.countByArticleIdAndIsDeletedFalse(articleId)).willReturn(3L);

    //DTO 변환결과 설정
    given(newsArticleViewMapper.toDto(savedArticleView, 3L)).willReturn(responseDto);


    //  when
    NewsArticleViewDto result = newsArticleService.createView(articleId, userId);

    //  then
    //반환 DTO 검증
    assertThat(result).isEqualTo(responseDto);

    verify(userRepository).findById(userId);
    verify(newsArticleRepository).findById(articleId);
    verify(articleViewRepository).existsByNewsArticleIdAndUserId(articleId, userId);
    verify(articleViewRepository).saveAndFlush(any(ArticleView.class));
    verify(commentRepository).countByArticleIdAndIsDeletedFalse(articleId);

    //조회수 증가가 수행되었는지 검증
    verify(newsArticleRepository).incrementViewCount(articleId);

    //DTO 변환이 수행되었는지 검증
    verify(newsArticleViewMapper).toDto(savedArticleView, 3L);
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