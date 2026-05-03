package com.springboot.monew.newsarticles.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import com.springboot.monew.newsarticles.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.RestoreResultDto;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.repository.ArticleInterestRepository;
import com.springboot.monew.newsarticles.repository.ArticleViewRepository;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.newsarticles.s3.NewsArticleRestoreService;
import com.springboot.monew.newsarticles.service.collector.ArticleCollector;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class NewsArticleIntegrationTest extends BaseIntegrationsTest {

  //실제 HTTP 요청을 보내기 위한 테스트용 RestTemplate
  @Autowired
  private TestRestTemplate restTemplate;

  //실제 DB 접근(PostgreSQL TestContainer)
  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private KeywordRepository keywordRepository;

  @Autowired
  private InterestKeywordRepository interestKeywordRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private ArticleViewRepository articleViewRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ArticleInterestRepository articleInterestRepository;

  @MockitoBean
  private NewsArticleRestoreService newsArticleRestoreService;

  // 모든 collector mock 처리
  @MockitoBean(name = "naverArticleCollector")
  private ArticleCollector naverArticleCollector;

  @MockitoBean(name = "chosunRssCollector")
  private ArticleCollector chosunRssCollector;

  @MockitoBean(name = "hankyungRssCollector")
  private ArticleCollector hankyungRssCollector;

  @MockitoBean(name = "yonhapRssCollector")
  private ArticleCollector yonhapRssCollector;

  @BeforeEach
  void cleanUp() {
    articleViewRepository.deleteAll();
    articleInterestRepository.deleteAll();
    newsArticleRepository.deleteAll();

    interestKeywordRepository.deleteAll();
    keywordRepository.deleteAll();
    interestRepository.deleteAll();
  }

  @Test
  @DisplayName("뉴스기사 수집 API 통합 테스트 - 키워드에 매칭된 기사만 DB에 저장된다.")
  void collectNews_SavesOnlyMatchedArticles_WhenKeywordMatches() {

    //  given
    // 1. 관심사 저장
    Interest interest = interestRepository.save(new Interest("게임"));

    // 2. 키워드 먼저 저장
    Keyword keyword = keywordRepository.save(new Keyword("롤"));

    // 3. 연결 저장
    InterestKeyword interestKeyword =
        interestKeywordRepository.save(new InterestKeyword(interest, keyword));

    // 3. collector가 어떤 source인지 설정
    // Naver로 설정
    given(naverArticleCollector.getSource()).willReturn(ArticleSource.NAVER);

    // 4. collector가 수집해온 기사 2개 (하나는 매칭, 하나는 미매칭)
    CollectedArticle matched = new CollectedArticle(
        ArticleSource.NAVER,
        "https://news.com/123456",
        "롤 페이커 우승", // ← 키워드 포함
        Instant.now(),
        "페이커 기사"
    );

    CollectedArticle unmatched = new CollectedArticle(
        ArticleSource.NAVER,
        "https://news.com/234",
        "스포츠 뉴스",
        Instant.now(),
        "축구 경기"
    );

    // collector.collect() 호출시 위 데이터 반환하도록 설정
    given(naverArticleCollector.collect(anyList())).willReturn(List.of(matched, unmatched));

    // 4. 나머지 collector는 빈 결과 반환 (외부 호출 방지)
    given(chosunRssCollector.getSource()).willReturn(ArticleSource.CHOSUN);
    given(hankyungRssCollector.getSource()).willReturn(ArticleSource.HANKYUNG);
    given(yonhapRssCollector.getSource()).willReturn(ArticleSource.YEONHAP);

    given(chosunRssCollector.collect(anyList())).willReturn(List.of());
    given(hankyungRssCollector.collect(anyList())).willReturn(List.of());
    given(yonhapRssCollector.collect(anyList())).willReturn(List.of());

    //  when
    ResponseEntity<String> response = restTemplate.postForEntity("/api/articles", null, String.class);

    //  then
    // 1. HTTP 응답 검증
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isEqualTo("뉴스 수집 완료");

    // 2. DB에 저장된 기사 조회
    List<NewsArticle> savedArticles = newsArticleRepository.findAll();

    assertThat(savedArticles)
        .filteredOn(article -> article.getOriginalLink().equals("https://news.com/123456"))
        .hasSize(1);

    assertThat(savedArticles)
        .noneMatch(article -> article.getOriginalLink().equals("https://news.com/234"));
  }

  @Test
  @DisplayName("뉴스기자 수집 API 통합테스트 - 키워드가 없으면 기사를 저장하지 않는다.")
  void collectNews_DoesNotSaveArticles_WhenKeywordDoesNotExist() {

    // given
    // 관심사 키워드를 저장하지 않는다.
    // 따라서 수집된 기사가 있어도 매칭될 키워드가 없다.
    given(naverArticleCollector.getSource()).willReturn(ArticleSource.NAVER);
    given(naverArticleCollector.collect(anyList()))
        .willReturn(List.of(
            new CollectedArticle(
                ArticleSource.NAVER,
                "https://news.com/1",
                "AI 기술 발전",
                Instant.now(),
                "인공지능 기사"
            )
        ));

    given(chosunRssCollector.getSource()).willReturn(ArticleSource.CHOSUN);
    given(hankyungRssCollector.getSource()).willReturn(ArticleSource.HANKYUNG);
    given(yonhapRssCollector.getSource()).willReturn(ArticleSource.YEONHAP);

    given(chosunRssCollector.collect(anyList())).willReturn(List.of());
    given(hankyungRssCollector.collect(anyList())).willReturn(List.of());
    given(yonhapRssCollector.collect(anyList())).willReturn(List.of());

    // when
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/articles", null, String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("뉴스 수집 완료");

    // 매칭 키워드가 없으므로 DB에는 저장되지 않아야 한다.
    assertThat(newsArticleRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("뉴스기사 수집 API 통합 테스트 - 이미 존재하는 원본 링크는 중복 저장하지 않는다.")
  void collectNews_DoesNotSaveDuplicateArticle_WhenOriginalLinkAlreadyExists() {
    // given
    //관심사 AI 저장
    Interest interest = interestRepository.save(new Interest("AI"));

    //키워드 AI 저장
    Keyword keyword = keywordRepository.save(new Keyword("AI"));

    //관심사 - 키워드 연결(AI - AI)
    interestKeywordRepository.save(new InterestKeyword(interest, keyword));

    // 기존 기사 저장
    NewsArticle existingArticle = NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("https://news.com/1")
        .title("AI 기존 기사")
        .publishedAt(Instant.now())
        .summary("기존 요약")
        .build();

    newsArticleRepository.save(existingArticle);

    //수집기를 NAVER 수집기로
    given(naverArticleCollector.getSource()).willReturn(ArticleSource.NAVER);
    given(naverArticleCollector.collect(anyList()))
        .willReturn(List.of(
            new CollectedArticle(
                ArticleSource.NAVER,
                "https://news.com/1", // 기존 기사와 같은 링크
                "AI 기술 발전",
                Instant.now(),
                "인공지능 기사"
            )
        ));

    //네이버만 수집, 나머지는 X로 설정
    given(chosunRssCollector.getSource()).willReturn(ArticleSource.CHOSUN);
    given(hankyungRssCollector.getSource()).willReturn(ArticleSource.HANKYUNG);
    given(yonhapRssCollector.getSource()).willReturn(ArticleSource.YEONHAP);

    given(chosunRssCollector.collect(anyList())).willReturn(List.of());
    given(hankyungRssCollector.collect(anyList())).willReturn(List.of());
    given(yonhapRssCollector.collect(anyList())).willReturn(List.of());

    // when
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/articles", null, String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 같은 sourceUrl/originalLink는 중복 저장되지 않아야 한다.
    assertThat(newsArticleRepository.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("기사 뷰 등록 API 통합 테스트 - 정상 요청 시 조회 이력이 저장되고 조회수가 증가한다")
  void createView_Returns201_WhenRequestIsValid() {

    // given
    // 1. 조회 이력을 등록할 사용자 저장
    User user = userRepository.save(
        new User("test@test.com", "테스트유저","1234")
    );

    // 2. 조회할 뉴스 기사 저장
    NewsArticle article = newsArticleRepository.save(
        NewsArticle.builder()
            .source(ArticleSource.NAVER)
            .originalLink("https://news.com/" + UUID.randomUUID())
            .title("AI 뉴스")
            .publishedAt(Instant.now())
            .summary("AI 관련 기사 요약")
            .build()
    );

    // 3. 사용자 식별 헤더 설정
    HttpHeaders headers = new HttpHeaders();
    headers.set("Monew-Request-User-ID", user.getId().toString());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    // when
    // 실제 REST API 호출
    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/articles/" + article.getId() + "/article-views",
        request,
        String.class
    );

    // then
    // 1. 응답 상태 코드 검증
    assertThat(response.getStatusCode().value()).isEqualTo(201);

    // 2. article_views 테이블에 조회 이력이 저장되었는지 검증
    boolean exists = articleViewRepository.existsByNewsArticleIdAndUserId(
        article.getId(),
        user.getId()
    );
    assertThat(exists).isTrue();

    // 3. 뉴스 기사 조회수가 1 증가했는지 검증
    NewsArticle updatedArticle = newsArticleRepository.findById(article.getId()).orElseThrow();
    assertThat(updatedArticle.getViewCount()).isEqualTo(1L);

    // 4. Location 헤더가 생성되었는지 검증
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().toString())
        .contains("/api/articles/" + article.getId() + "/article-views/");
  }

  @Test
  @DisplayName("기사 뷰 등록 API 통합 테스트 - 삭제된 기사 조회 시 조회 이력을 저장하지 않는다")
  void createView_ReturnsError_WhenArticleIsDeleted() {

    // given
    User user = userRepository.save(
        new User("test3@test.com", "테스트유저3","1234")
    );

    NewsArticle article = newsArticleRepository.save(
        NewsArticle.builder()
            .source(ArticleSource.NAVER)
            .originalLink("https://news.com/3")
            .title("삭제된 뉴스")
            .publishedAt(Instant.now())
            .summary("삭제된 기사 요약")
            .build()
    );

    // 뉴스기사 논리삭제
    article.delete();
    newsArticleRepository.saveAndFlush(article);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Monew-Request-User-ID", user.getId().toString());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/articles/" + article.getId() + "/article-views",
        HttpMethod.POST, request, ErrorResponse.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("NA02");
    assertThat(response.getBody().getMessage()).isEqualTo("이미 삭제된 뉴스기사입니다.");

    // 삭제된 기사이므로 조회 이력이 생성되면 안 된다.
    boolean exists = articleViewRepository.existsByNewsArticleIdAndUserId(
        article.getId(),
        user.getId()
    );
    assertThat(exists).isFalse();

    // 조회수도 증가하면 안 된다.
    NewsArticle updatedArticle = newsArticleRepository.findById(article.getId()).orElseThrow();
    assertThat(updatedArticle.getViewCount()).isEqualTo(0L);
  }

  @Test
  @DisplayName("뉴스기사 목록조회 API 통합 테스트 - 정상 요청시 페이지 응답을 반환한다.")
  void list_ReturnsPageResponse_WhenRequestIsValid() {
    //  given
    User user = userRepository.save(new User("kis2690@naver.com", "꽉스","2690"));

    newsArticleRepository.save(
        NewsArticle.builder()
            .source(ArticleSource.NAVER)
            .originalLink("https://news.com/list-1")
            .title("손흥민 월드컵 뛸 수 있나?")
            .publishedAt(Instant.now())
            .summary("손흥민 월드컵 기사")
            .build()
    );

    HttpHeaders headers = new HttpHeaders();
    headers.set("Monew-Request-User-ID", user.getId().toString());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    //  when
    ResponseEntity<CursorPageResponseNewsArticleDto> response =
        restTemplate.exchange(
            "/api/articles?limit=10&orderBy=publishDate&direction=DESC",
            HttpMethod.GET,
            request,
            CursorPageResponseNewsArticleDto.class
        );

    //  then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().content()).isNotEmpty();
    assertThat(response.getBody().content()).hasSize(1);
  }

  @Test
  @DisplayName("뉴스기사 단건 조회 API 통합 테스트 - 정상 요청 시 기사 정보를 반환한다")
  void find_ReturnsArticle_WhenRequestIsValid() {

    // given
    User user = userRepository.save(
        new User("find@test.com", "단건조회유저", "1234")
    );

    NewsArticle article = newsArticleRepository.save(
        NewsArticle.builder()
            .source(ArticleSource.NAVER)
            .originalLink("https://news.com/find-1")
            .title("단건 조회 기사")
            .publishedAt(Instant.now())
            .summary("단건 조회 요약")
            .build()
    );

    HttpHeaders headers = new HttpHeaders();
    headers.set("Monew-Request-User-ID", user.getId().toString());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    // when
    ResponseEntity<NewsArticleDto> response =
        restTemplate.exchange(
            "/api/articles/" + article.getId(),
            HttpMethod.GET,
            request,
            NewsArticleDto.class
        );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(article.getId());
    assertThat(response.getBody().title()).isEqualTo("단건 조회 기사");
  }

  @Test
  @DisplayName("출처 목록 조회 API 통합 테스트 - 정상 요청 시 출처 목록을 반환한다")
  void findSource_ReturnsSources_WhenRequestIsValid() {

    // when
    ResponseEntity<List<ArticleSource>> response =
        restTemplate.exchange(
            "/api/articles/sources",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {}
        );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains(ArticleSource.NAVER);
  }

  @Test
  @DisplayName("뉴스기사 복구 API 통합 테스트 - 정상 요청 시 복구 결과를 반환한다")
  void restore_ReturnsRestoreResults_WhenRequestIsValid() {

    // given
    given(newsArticleRestoreService.restore(any(LocalDate.class), any(LocalDate.class)))
        .willReturn(List.of());

    // when
    ResponseEntity<List<RestoreResultDto>> response =
        restTemplate.exchange(
            "/api/articles/restore?from=2026-01-01&to=2026-01-31",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {}
        );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    verify(newsArticleRestoreService).restore(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
    );
  }

  @Test
  @DisplayName("뉴스기사 논리삭제 API 통합 테스트 - 정상 요청 시 204를 반환하고 삭제 상태가 된다")
  void softDelete_ReturnsNoContent_WhenArticleExists() {

    // given
    NewsArticle article = newsArticleRepository.save(
        NewsArticle.builder()
            .source(ArticleSource.NAVER)
            .originalLink("https://news.com/delete-soft-1")
            .title("논리삭제 기사")
            .publishedAt(Instant.now())
            .summary("논리삭제 요약")
            .build()
    );

    // when
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/api/articles/" + article.getId(),
            HttpMethod.DELETE,
            null,
            Void.class
        );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    NewsArticle deletedArticle =
        newsArticleRepository.findById(article.getId()).orElseThrow();

    assertThat(deletedArticle.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("뉴스기사 물리삭제 API 통합 테스트 - 정상 요청 시 204를 반환하고 DB에서 삭제된다")
  void hardDelete_ReturnsNoContent_WhenArticleExists() {

    // given
    NewsArticle article = newsArticleRepository.save(
        NewsArticle.builder()
            .source(ArticleSource.NAVER)
            .originalLink("https://news.com/delete-hard-1")
            .title("물리삭제 기사")
            .publishedAt(Instant.now())
            .summary("물리삭제 요약")
            .build()
    );

    // when
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/api/articles/" + article.getId() + "/hard",
            HttpMethod.DELETE,
            null,
            Void.class
        );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(newsArticleRepository.findById(article.getId())).isEmpty();
  }
}
