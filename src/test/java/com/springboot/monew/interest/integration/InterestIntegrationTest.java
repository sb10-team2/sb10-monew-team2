package com.springboot.monew.interest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.CursorPageResponseInterestDto;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.interest.repository.SubscriptionRepository;
import com.springboot.monew.newsarticles.repository.ArticleInterestRepository;
import com.springboot.monew.newsarticles.repository.ArticleViewRepository;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.notification.repository.NotificationRepository;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class InterestIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleViewRepository articleViewRepository;

  @Autowired
  private ArticleInterestRepository articleInterestRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private KeywordRepository keywordRepository;

  @Autowired
  private InterestKeywordRepository interestKeywordRepository;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    commentLikeRepository.deleteAll();
    commentRepository.deleteAll();
    articleViewRepository.deleteAll();
    articleInterestRepository.deleteAll();
    subscriptionRepository.deleteAll();
    interestKeywordRepository.deleteAll();
    newsArticleRepository.deleteAll();
    keywordRepository.deleteAll();
    interestRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("관심사 생성 요청이 유효하면 관심사 응답을 반환한다")
  void create_ReturnsInterestDto_WhenValidRequest() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("경제", List.of("주식", "채권"));

    // when
    ResponseEntity<InterestDto> response = restTemplate.postForEntity(
        "/api/interests",
        jsonEntity(request),
        InterestDto.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    InterestDto body = response.getBody();
    assertThat(body.id()).isNotNull();
    assertThat(body.name()).isEqualTo("경제");
    assertThat(body.keywords()).containsExactlyInAnyOrder("주식", "채권");
    assertThat(body.subscriberCount()).isZero();
    assertThat(body.subscribedByMe()).isFalse();

    assertThat(interestRepository.findById(body.id())).isPresent();
    assertThat(keywordRepository.findByName("주식")).isPresent();
    assertThat(keywordRepository.findByName("채권")).isPresent();
    assertThat(interestKeywordRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("관심사 이름이 중복되면 예외 응답을 반환한다")
  void create_ThrowsException_WhenNameAlreadyExists() {
    // given
    saveInterestWithKeywords("경제", List.of("주식"));
    InterestRegisterRequest request = new InterestRegisterRequest("경제", List.of("환율"));

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/interests",
        jsonEntity(request),
        String.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(errorCode(response)).isEqualTo("IN01");
  }

  @Test
  @DisplayName("관심사 목록 조회 결과에 다음 페이지가 없으면 현재 페이지 응답을 반환한다")
  void list_ReturnsCursorPage_WhenHasNextFalse() {
    // given
    User user = saveUser("list@example.com", "list-user");
    Interest economy = saveInterestWithKeywords("경제", List.of("주식", "거시경제"));
    saveInterestWithKeywords("스포츠", List.of("축구"));
    subscribeInterest(user, economy);

    // when
    ResponseEntity<CursorPageResponseInterestDto> response = restTemplate.exchange(
        "/api/interests?keyword={keyword}&orderBy=name&direction=ASC&limit={limit}",
        HttpMethod.GET,
        userHeader(user.getId()),
        CursorPageResponseInterestDto.class,
        "주식",
        10
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    CursorPageResponseInterestDto body = response.getBody();
    assertThat(body.content()).hasSize(1);
    assertThat(body.content().get(0).id()).isEqualTo(economy.getId());
    assertThat(body.content().get(0).name()).isEqualTo("경제");
    assertThat(body.content().get(0).keywords()).containsExactlyInAnyOrder("주식", "거시경제");
    assertThat(body.content().get(0).subscriberCount()).isEqualTo(1L);
    assertThat(body.content().get(0).subscribedByMe()).isTrue();
    assertThat(body.size()).isEqualTo(1);
    assertThat(body.totalElements()).isEqualTo(1L);
    assertThat(body.hasNext()).isFalse();
  }

  @Test
  @DisplayName("관심사 목록 조회 결과에 다음 페이지가 있으면 다음 커서를 포함한 페이지 응답을 반환한다")
  void list_ReturnsCursorPageWithNextCursor_WhenHasNextTrue() {
    // given
    User user = saveUser("cursor@example.com", "cursor-user");
    saveInterestWithKeywords("경제", List.of("주식"));
    saveInterestWithKeywords("문화", List.of("영화"));
    saveInterestWithKeywords("여행", List.of("항공권"));

    // when
    ResponseEntity<CursorPageResponseInterestDto> response = restTemplate.exchange(
        "/api/interests?orderBy=name&direction=ASC&limit=2",
        HttpMethod.GET,
        userHeader(user.getId()),
        CursorPageResponseInterestDto.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    CursorPageResponseInterestDto body = response.getBody();
    assertThat(body.content()).hasSize(2);
    List<String> contentNames = body.content().stream()
        .map(InterestDto::name)
        .toList();
    assertThat(contentNames)
        .doesNotHaveDuplicates()
        .allMatch(List.of("경제", "문화", "여행")::contains);
    assertThat(body.nextCursor()).isEqualTo(body.content().get(1).name());
    assertThat(body.nextAfter()).isNotNull();
    assertThat(body.size()).isEqualTo(2);
    assertThat(body.totalElements()).isEqualTo(3L);
    assertThat(body.hasNext()).isTrue();
  }

  @Test
  @DisplayName("관심사 수정 요청이 유효하면 수정된 관심사 응답을 반환한다")
  void update_ReturnsUpdatedInterestDto_WhenValidRequest() {
    // given
    Interest interest = saveInterestWithKeywords("기술", List.of("인공지능", "클라우드"));
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("인공지능", "로봇"));

    // when
    ResponseEntity<InterestDto> response = restTemplate.exchange(
        "/api/interests/" + interest.getId(),
        HttpMethod.PATCH,
        jsonEntity(request),
        InterestDto.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(interest.getId());
    assertThat(response.getBody().name()).isEqualTo("기술");
    assertThat(response.getBody().keywords()).containsExactlyInAnyOrder("인공지능", "로봇");

    List<String> keywordNames = interestKeywordRepository.findAllByInterestWithKeyword(interest)
        .stream()
        .map(InterestKeyword::getKeyword)
        .map(Keyword::getName)
        .toList();

    assertThat(keywordNames).containsExactlyInAnyOrder("인공지능", "로봇");
    assertThat(keywordRepository.findByName("클라우드")).isEmpty();
    assertThat(keywordRepository.findByName("인공지능")).isPresent();
    assertThat(keywordRepository.findByName("로봇")).isPresent();
  }

  @Test
  @DisplayName("관심사 수정 요청의 키워드가 중복되면 예외 응답을 반환한다")
  void update_ThrowsException_WhenKeywordsDuplicated() {
    // given
    Interest interest = saveInterestWithKeywords("기술", List.of("인공지능", "클라우드"));
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("인공지능", "인공지능"));

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        "/api/interests/" + interest.getId(),
        HttpMethod.PATCH,
        jsonEntity(request),
        String.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(errorCode(response)).isEqualTo("IN02");
  }

  @Test
  @DisplayName("관심사 수정 시 다른 관심사에서 사용 중인 키워드는 삭제하지 않는다")
  void update_DoesNotDeleteSharedKeyword_WhenKeywordUsedByAnotherInterest() {
    // given
    Interest technology = saveInterestWithKeywords("기술", List.of("인공지능", "클라우드"));
    saveInterestWithKeywords("과학", List.of("인공지능"));

    // when
    ResponseEntity<InterestDto> response = restTemplate.exchange(
        "/api/interests/" + technology.getId(),
        HttpMethod.PATCH,
        jsonEntity(new InterestUpdateRequest(List.of("클라우드"))),
        InterestDto.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(technology.getId());
    assertThat(response.getBody().name()).isEqualTo("기술");
    assertThat(response.getBody().keywords()).containsExactly("클라우드");

    List<String> technologyKeywordNames = interestKeywordRepository
        .findAllByInterestWithKeyword(technology)
        .stream()
        .map(InterestKeyword::getKeyword)
        .map(Keyword::getName)
        .toList();

    assertThat(technologyKeywordNames).containsExactly("클라우드");
    assertThat(keywordRepository.findByName("인공지능")).isPresent();
    assertThat(keywordRepository.findByName("클라우드")).isPresent();
  }

  @Test
  @DisplayName("관심사 구독 요청이 유효하면 구독 응답을 반환한다")
  void subscribe_ReturnsSubscriptionDto_WhenValidRequest() {
    // given
    User user = saveUser("subscribe@example.com", "subscriber");
    Interest interest = saveInterestWithKeywords("문화", List.of("영화"));

    // when
    ResponseEntity<SubscriptionDto> response = restTemplate.exchange(
        "/api/interests/" + interest.getId() + "/subscriptions",
        HttpMethod.POST,
        userHeader(user.getId()),
        SubscriptionDto.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().interestId()).isEqualTo(interest.getId());
    assertThat(response.getBody().interestName()).isEqualTo("문화");
    assertThat(response.getBody().interestKeywords()).containsExactly("영화");
    assertThat(response.getBody().interestSubscriberCount()).isEqualTo(1L);
    assertThat(subscriptionRepository.existsByUserIdAndInterestId(user.getId(), interest.getId()))
        .isTrue();
    assertThat(interestRepository.findSubscriberCountById(interest.getId())).isEqualTo(1L);
  }

  @Test
  @DisplayName("이미 구독한 관심사를 다시 구독하면 예외 응답을 반환한다")
  void subscribe_ThrowsException_WhenAlreadySubscribed() {
    // given
    User user = saveUser("already@example.com", "already-subscriber");
    Interest interest = saveInterestWithKeywords("문화", List.of("영화"));
    subscribeInterest(user, interest);

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        "/api/interests/" + interest.getId() + "/subscriptions",
        HttpMethod.POST,
        userHeader(user.getId()),
        String.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(errorCode(response)).isEqualTo("IN04");
  }

  @Test
  @DisplayName("구독 중인 관심사를 구독 취소하면 구독 정보가 삭제된다")
  void unsubscribe_DeletesSubscription_WhenSubscriptionExists() {
    // given
    User user = saveUser("unsubscribe@example.com", "unsubscriber");
    Interest interest = saveInterestWithKeywords("문화", List.of("영화"));
    subscribeInterest(user, interest);

    // when
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/interests/" + interest.getId() + "/subscriptions",
        HttpMethod.DELETE,
        userHeader(user.getId()),
        Void.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(subscriptionRepository.existsByUserIdAndInterestId(user.getId(), interest.getId()))
        .isFalse();
    assertThat(interestRepository.findSubscriberCountById(interest.getId())).isEqualTo(0L);
  }

  @Test
  @DisplayName("구독하지 않은 관심사를 구독 취소하면 예외 응답을 반환한다")
  void unsubscribe_ThrowsException_WhenSubscriptionNotFound() {
    // given
    User user = saveUser("missing-subscription@example.com", "missing-subscriber");
    Interest interest = saveInterestWithKeywords("문화", List.of("영화"));

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        "/api/interests/" + interest.getId() + "/subscriptions",
        HttpMethod.DELETE,
        userHeader(user.getId()),
        String.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(errorCode(response)).isEqualTo("IN05");
  }

  @Test
  @DisplayName("관심사를 삭제하면 관심사와 더 이상 사용되지 않는 키워드가 삭제된다")
  void delete_DeletesInterestAndOrphanKeywords_WhenInterestExists() {
    // given
    Interest interest = saveInterestWithKeywords("여행", List.of("호텔", "항공권"));

    // when
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/interests/" + interest.getId(),
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        Void.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(interestRepository.findById(interest.getId())).isEmpty();
    assertThat(interestKeywordRepository.count()).isZero();
    assertThat(keywordRepository.findByName("호텔")).isEmpty();
    assertThat(keywordRepository.findByName("항공권")).isEmpty();
  }

  @Test
  @DisplayName("존재하지 않는 관심사를 삭제하면 예외 응답을 반환한다")
  void delete_ThrowsException_WhenInterestNotFound() {
    // given
    UUID missingInterestId = UUID.randomUUID();

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        "/api/interests/" + missingInterestId,
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        String.class
    );

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(errorCode(response)).isEqualTo("IN03");
  }

  private void subscribeInterest(User user, Interest interest) {
    ResponseEntity<SubscriptionDto> response = restTemplate.exchange(
        "/api/interests/" + interest.getId() + "/subscriptions",
        HttpMethod.POST,
        userHeader(user.getId()),
        SubscriptionDto.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private User saveUser(String email, String nickname) {
    return userRepository.saveAndFlush(new User(email, nickname, "password1234!"));
  }

  private Interest saveInterestWithKeywords(String interestName, List<String> keywordNames) {
    Interest interest = interestRepository.saveAndFlush(new Interest(interestName));
    for (String keywordName : keywordNames) {
      Keyword keyword = keywordRepository.findByName(keywordName)
          .orElseGet(() -> keywordRepository.saveAndFlush(new Keyword(keywordName)));
      interestKeywordRepository.saveAndFlush(new InterestKeyword(interest, keyword));
    }
    return interest;
  }

  private String errorCode(ResponseEntity<String> response) {
    try {
      return objectMapper.readTree(response.getBody()).path("code").asText();
    } catch (Exception e) {
      throw new AssertionError("오류 응답 JSON을 파싱할 수 없습니다: " + response.getBody(), e);
    }
  }

  private <T> HttpEntity<T> jsonEntity(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }

  private HttpEntity<Void> userHeader(UUID userId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Monew-Request-User-ID", userId.toString());
    return new HttpEntity<>(headers);
  }
}
