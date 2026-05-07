package com.springboot.monew.newsarticle.integration;

import static org.assertj.core.api.Assertions.assertThat;
import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.newsarticle.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticle.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import com.springboot.monew.newsarticle.enums.ArticleSource;
import com.springboot.monew.newsarticle.repository.ArticleInterestRepository;
import com.springboot.monew.newsarticle.repository.ArticleViewRepository;
import com.springboot.monew.newsarticle.repository.NewsArticleRepository;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
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

  @BeforeEach
  void cleanUp() {
    articleViewRepository.deleteAllInBatch();
    articleInterestRepository.deleteAllInBatch();

    newsArticleRepository.deleteAllInBatch();

    interestKeywordRepository.deleteAllInBatch();
    keywordRepository.deleteAllInBatch();
    interestRepository.deleteAllInBatch();

    userRepository.deleteAllInBatch();
  }

  //mokitoBean 설정을 할 수 없어 수집 통합테스트 불가
  //mokitoBean 사용하기위해서는 schema.sql 사용 x

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
