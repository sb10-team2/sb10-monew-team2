package com.springboot.monew.user.integration.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.interest.repository.SubscriptionRepository;
import com.springboot.monew.interest.service.InterestService;
import com.springboot.monew.interest.service.SubscriptionService;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.response.UserDto;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.repository.UserActivityRepository;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserActivityInterestIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private UserService userService;

  @Autowired
  private InterestService interestService;

  @Autowired
  private SubscriptionService subscriptionService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserActivityRepository userActivityRepository;

  @Autowired
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Autowired
  private InterestKeywordRepository interestKeywordRepository;

  @Autowired
  private KeywordRepository keywordRepository;

  @Autowired
  private InterestRepository interestRepository;

  private UserDto user;

  @BeforeEach
  void setUp() {
    // 테스트 간 데이터 격리를 위해 Mongo 활동 문서와 관심사 관련 RDB 데이터를 모두 초기화한다.
    userActivityRepository.deleteAll();
    userActivityOutboxRepository.deleteAll();
    subscriptionRepository.deleteAll();
    interestKeywordRepository.deleteAll();
    keywordRepository.deleteAll();
    interestRepository.deleteAll();
    userRepository.deleteAll();

    // 관심사 흐름 검증 전, 실제 회원가입 서비스로 사용자와 사용자 활동 문서를 먼저 생성한다.
    user = userService.register(new UserRegisterRequest(
        "interest@test.com",
        "interestUser",
        "password123!"
    ));

    // 회원가입 시 생성된 USER_REGISTERED Outbox 이벤트는 관심사 흐름 검증 대상에서 제외하기 위해 비운다.
    userActivityOutboxRepository.deleteAll();
  }

  @Test
  @DisplayName("관심사 구독 이후 사용자 활동 문서에 구독 정보와 INTEREST_SUBSCRIBED Outbox 이벤트가 추가된다")
  void subscribe_addsSubscriptionToUserActivityAndCreatesOutbox() {
    // given
    InterestDto interest = createInterest("금융", List.of("주식", "채권"));

    // when
    // 실제 구독 서비스 진입점을 호출해 구독 저장, Outbox 저장, 활동 문서 반영을 한 흐름으로 검증한다.
    SubscriptionDto subscription = subscriptionService.subscribe(interest.id(), user.id());

    // then
    assertThat(subscription.interestId()).isEqualTo(interest.id());
    assertThat(subscription.interestName()).isEqualTo(interest.name());
    assertThat(subscription.interestKeywords()).containsExactly("주식", "채권");

    // 관심사 구독 후 INTEREST_SUBSCRIBED Outbox 이벤트가 1건 저장되어야 한다.
    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.INTEREST_SUBSCRIBED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.SUBSCRIPTION);
    assertThat(outbox.getAggregateId()).isEqualTo(subscription.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getPayload()).contains(interest.name());
    assertThat(outbox.getPayload()).contains("주식");
    assertThat(outbox.getPayload()).contains("채권");

    // Mongo 사용자 활동 문서에 구독 정보가 추가되어야 한다.
    UserActivityDocument activity = userActivityRepository.findById(user.id()).orElseThrow();
    assertThat(activity.getSubscriptions()).hasSize(1);
    assertThat(activity.getSubscriptions().get(0).interestId()).isEqualTo(interest.id());
    assertThat(activity.getSubscriptions().get(0).interestName()).isEqualTo(interest.name());
    assertThat(activity.getSubscriptions().get(0).interestKeywords())
        .containsExactly("주식", "채권");
  }

  @Test
  @DisplayName("관심사 구독 취소 이후 사용자 활동 문서에서 구독 정보가 제거되고 INTEREST_UNSUBSCRIBED Outbox 이벤트가 저장된다")
  void unsubscribe_removesSubscriptionFromUserActivityAndCreatesOutbox() {
    // given
    InterestDto interest = createInterest("부동산", List.of("청약", "분양"));
    subscriptionService.subscribe(interest.id(), user.id());

    // 구독 시 생성된 INTEREST_SUBSCRIBED Outbox 이벤트는 구독 취소 검증 대상에서 제외하기 위해 비운다.
    userActivityOutboxRepository.deleteAll();

    // when
    // 실제 구독 취소 서비스 진입점을 호출해 구독 삭제, Outbox 저장, 활동 문서 반영을 한 흐름으로 검증한다.
    subscriptionService.unsubscribe(interest.id(), user.id());

    // then
    // 관심사 구독 취소 후 INTEREST_UNSUBSCRIBED Outbox 이벤트가 1건 저장되어야 한다.
    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.INTEREST_UNSUBSCRIBED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.INTEREST);
    assertThat(outbox.getAggregateId()).isEqualTo(interest.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);

    // Mongo 사용자 활동 문서에서 구독 정보가 제거되어야 한다.
    UserActivityDocument activity = userActivityRepository.findById(user.id()).orElseThrow();
    assertThat(activity.getSubscriptions()).isEmpty();
  }

  @Test
  @DisplayName("관심사 수정 이후 해당 관심사를 구독 중인 사용자 활동 문서들의 키워드와 INTEREST_UPDATED Outbox 이벤트가 함께 갱신된다")
  void updateInterest_updatesSubscriptionKeywordsInUserActivityAndCreatesOutbox() {
    // given
    InterestDto interest = createInterest("테크", List.of("AI", "로봇"));
    subscriptionService.subscribe(interest.id(), user.id());

    // 구독 시 생성된 INTEREST_SUBSCRIBED Outbox 이벤트는 관심사 수정 검증 대상에서 제외하기 위해 비운다.
    userActivityOutboxRepository.deleteAll();

    InterestUpdateRequest request = new InterestUpdateRequest(List.of("반도체", "클라우드"));

    // when
    // 실제 관심사 수정 서비스 진입점을 호출해 Outbox 저장과 구독 중인 활동 문서 키워드 갱신을 함께 검증한다.
    InterestDto updated = interestService.update(interest.id(), request);

    // then
    assertThat(updated.id()).isEqualTo(interest.id());
    assertThat(updated.keywords()).containsExactly("반도체", "클라우드");

    // 관심사 수정 후 INTEREST_UPDATED Outbox 이벤트가 1건 저장되어야 한다.
    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.INTEREST_UPDATED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.INTEREST);
    assertThat(outbox.getAggregateId()).isEqualTo(interest.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getPayload()).contains("반도체");
    assertThat(outbox.getPayload()).contains("클라우드");

    // 해당 관심사를 구독 중인 Mongo 사용자 활동 문서의 키워드가 최신 값으로 갱신되어야 한다.
    UserActivityDocument activity = userActivityRepository.findById(user.id()).orElseThrow();
    assertThat(activity.getSubscriptions()).hasSize(1);
    assertThat(activity.getSubscriptions().get(0).interestId()).isEqualTo(interest.id());
    assertThat(activity.getSubscriptions().get(0).interestKeywords())
        .containsExactly("반도체", "클라우드");
  }

  private InterestDto createInterest(String name, List<String> keywords) {
    // 관심사 흐름 테스트에서 공통으로 사용할 관심사를 실제 생성 서비스로 준비한다.
    return interestService.create(new InterestRegisterRequest(name, keywords));
  }

  @Test
  @DisplayName("관심사 구독이 10개를 넘어도 사용자 활동내역 문서에는 전체 구독 목록이 유지된다")
  void subscribe_keepsAllSubscriptionsInUserActivity() {
    // given
    List<InterestDto> createdInterests = new ArrayList<>();

    // when
    // 서로 유사한 이름으로 판정되지 않도록 UUID를 섞어서 관심사를 생성하고 바로 구독한다.
    for (int i = 0; i < 11; i++) {
      InterestDto interest = createInterest(
          "관심사-" + UUID.randomUUID(),
          List.of("keyword-" + i)
      );
      createdInterests.add(interest);
      subscriptionService.subscribe(interest.id(), user.id());
    }

    // then
    // 사용자 활동내역 Mongo 문서를 조회한다.
    UserActivityDocument activity = userActivityRepository.findById(user.id()).orElseThrow();

    // subscriptions는 더 이상 10개로 잘리지 않고 전체 11개가 유지되어야 한다.
    assertThat(activity.getSubscriptions()).hasSize(11);

    // 가장 마지막에 구독한 관심사가 맨 앞에 있어야 한다.
    assertThat(activity.getSubscriptions().get(0).interestId())
        .isEqualTo(createdInterests.get(10).id());

    // 가장 먼저 구독한 관심사는 맨 뒤에 있어야 한다.
    assertThat(activity.getSubscriptions().get(10).interestId())
        .isEqualTo(createdInterests.get(0).id());
  }
}
