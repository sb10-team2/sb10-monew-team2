package com.springboot.monew.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.interest.exception.InterestErrorCode;
import com.springboot.monew.interest.exception.InterestException;
import com.springboot.monew.interest.mapper.SubscriptionDtoMapper;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.SubscriptionRepository;
import com.springboot.monew.user.document.UserActivityDocument.SubscriptionItem;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.event.interest.InterestSubscribedEvent;
import com.springboot.monew.user.event.interest.InterestUnsubscribedEvent;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private InterestKeywordRepository interestKeywordRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SubscriptionDtoMapper subscriptionDtoMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private SubscriptionService subscriptionService;

  @Test
  @DisplayName("관심사를 구독할 수 있다")
  void subscribe_ReturnsSubscriptionDto_WhenValidRequest() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");
    Interest interest = interest(interestId, "금융");
    Subscription subscription = subscription(
        UUID.randomUUID(),
        user,
        interest,
        Instant.parse("2026-04-20T00:00:00Z")
    );
    InterestKeyword firstLink = new InterestKeyword(interest, keyword(UUID.randomUUID(), "주식"));
    InterestKeyword secondLink = new InterestKeyword(interest, keyword(UUID.randomUUID(), "채권"));
    SubscriptionDto expected = new SubscriptionDto(
        subscription.getId(),
        interestId,
        "금융",
        List.of("주식", "채권"),
        2L,
        subscription.getCreatedAt()
    );

    // 사용자 활동 이벤트 발행 시 InterestSubscribedEvent 안에 담길 SubscriptionItem을 미리 준비한다.
    SubscriptionItem subscriptionItem = new SubscriptionItem(
        subscription.getId(),
        interestId,
        interest.getName(),
        List.of("주식", "채권"),
        subscription.getCreatedAt()
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(false);
    given(subscriptionRepository.saveAndFlush(any(Subscription.class))).willReturn(subscription);
    given(interestRepository.incrementSubscriberCount(interestId)).willReturn(1);
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(List.of(firstLink, secondLink));
    given(interestRepository.findSubscriberCountById(interestId)).willReturn(2L);
    given(subscriptionDtoMapper.toSubscriptionDto(subscription, List.of("주식", "채권"), 2L))
        .willReturn(expected);
    // subscribe() 내부에서 InterestSubscribedEvent 생성 시 toSubscriptionItem()을 호출하므로 null이 반환되지 않도록 stub 처리한다.
    given(subscriptionDtoMapper.toSubscriptionItem(subscription, List.of("주식", "채권"))).willReturn(subscriptionItem);

    // when
    SubscriptionDto result = subscriptionService.subscribe(interestId, userId);

    // then
    assertThat(result).isEqualTo(expected);

    // 발행된 InterestSubscribedEvent를 가져와 구독 정보가 올바르게 담겼는지 검증한다.
    ArgumentCaptor<InterestSubscribedEvent> captor =
        ArgumentCaptor.forClass(InterestSubscribedEvent.class);

    // eventPublisher.publishEvent()가 호출되었는지 확인하고 전달된 이벤트 객체를 가져온다.
    verify(eventPublisher).publishEvent(captor.capture());

    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().item().interestId()).isEqualTo(interestId);
    assertThat(captor.getValue().item().interestName()).isEqualTo(interest.getName());
    assertThat(captor.getValue().item().interestKeywords()).containsExactly("주식", "채권");
  }

  @Test
  @DisplayName("존재하지 않는 사용자가 구독하면 예외가 발생한다")
  void subscribe_ThrowsException_WhenUserNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(interestRepository, never()).findById(any());
  }

  @Test
  @DisplayName("삭제된 사용자가 구독하면 예외가 발생한다")
  void subscribe_ThrowsException_WhenUserDeleted() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User deletedUser = new User("deleted@example.com", "tester", "password");
    deletedUser.delete();

    given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(interestRepository, never()).findById(any());
  }

  @Test
  @DisplayName("존재하지 않는 관심사를 구독하면 예외가 발생한다")
  void subscribe_ThrowsException_WhenInterestNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.empty());

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId));
        });
  }

  @Test
  @DisplayName("이미 구독한 관심사를 다시 구독하면 예외가 발생한다")
  void subscribe_ThrowsException_WhenSubscriptionAlreadyExists() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");
    Interest interest = interest(interestId, "금융");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(true);

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId, "userId", userId));
        });

    verify(subscriptionRepository, never()).saveAndFlush(any(Subscription.class));
  }

  @Test
  @DisplayName("구독 저장 중 무결성 예외가 발생하고 실제로 중복 구독이면 예외로 변환한다")
  void subscribe_ThrowsException_WhenIntegrityViolationFromDuplicateSubscription() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");
    Interest interest = interest(interestId, "산업");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId))
        .willReturn(false, true);
    given(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .willThrow(new DataIntegrityViolationException("duplicate"));

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId, "userId", userId));
        });
  }

  @Test
  @DisplayName("구독 저장 중 무결성 예외가 발생하고 관심사가 없으면 예외로 변환한다")
  void subscribe_ThrowsException_WhenIntegrityViolationFromInterestNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");
    Interest interest = interest(interestId, "테크");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId))
        .willReturn(false, false);
    given(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .willThrow(new DataIntegrityViolationException("fk"));
    given(interestRepository.existsById(interestId)).willReturn(false);

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId));
        });
  }

  @Test
  @DisplayName("구독 저장 중 예상 밖의 무결성 예외가 발생하면 원래 예외를 다시 던진다")
  void subscribe_RethrowsDataIntegrityViolationException_WhenUnexpectedPersistenceConflictOccurs() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");
    Interest interest = new Interest("금융");
    DataIntegrityViolationException expectedException =
        new DataIntegrityViolationException("unexpected conflict");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId))
        .willReturn(false);
    given(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .willThrow(expectedException);
    given(interestRepository.existsById(interestId)).willReturn(true);

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isSameAs(expectedException);

    verify(interestRepository, never()).incrementSubscriberCount(interestId);
  }

  @Test
  @DisplayName("구독자 수 증가 결과가 0이면 예외가 발생한다")
  void subscribe_ThrowsException_WhenSubscriberCountIncrementFails() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");
    Interest interest = interest(interestId, "부동산");
    Subscription subscription = subscription(
        UUID.randomUUID(),
        user,
        interest,
        Instant.parse("2026-04-20T00:00:00Z")
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(false);
    given(subscriptionRepository.saveAndFlush(any(Subscription.class))).willReturn(subscription);
    given(interestRepository.incrementSubscriberCount(interestId)).willReturn(0);

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId));
        });

    verify(subscriptionDtoMapper, never()).toSubscriptionDto(any(), any(), anyLong());
  }

  @Test
  @DisplayName("구독자 수 조회 결과가 없으면 예외가 발생한다")
  void subscribe_ThrowsException_WhenSubscriberCountIsNull() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");
    Interest interest = interest(interestId, "문화");
    Subscription subscription = subscription(
        UUID.randomUUID(),
        user,
        interest,
        Instant.parse("2026-04-20T00:00:00Z")
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(false);
    given(subscriptionRepository.saveAndFlush(any(Subscription.class))).willReturn(subscription);
    given(interestRepository.incrementSubscriberCount(interestId)).willReturn(1);
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(List.of());
    given(interestRepository.findSubscriberCountById(interestId)).willReturn(null);

    // when
    ThrowingCallable action = () -> subscriptionService.subscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId));
        });
  }

  @Test
  @DisplayName("관심사 구독을 취소할 수 있다")
  void unsubscribe_DeletesSubscription_WhenValidRequest() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId)).willReturn(1);
    given(interestRepository.decrementSubscriberCount(interestId)).willReturn(1);

    // when
    subscriptionService.unsubscribe(interestId, userId);

    // then
    verify(subscriptionRepository).deleteByUserIdAndInterestId(userId, interestId);
    verify(interestRepository).decrementSubscriberCount(interestId);

    // 발행된 InterestUnsubscribedEvent를 가져와 구독 취소 대상 정보가 올바르게 담겼는지 검증한다.
    ArgumentCaptor<InterestUnsubscribedEvent> captor =
        ArgumentCaptor.forClass(InterestUnsubscribedEvent.class);

    // eventPublisher.publishEvent()가 호출되었는지 확인하고 전달된 이벤트 객체를 가져온다.
    verify(eventPublisher).publishEvent(captor.capture());

    assertThat(captor.getValue().userId()).isEqualTo(userId);
    assertThat(captor.getValue().interestId()).isEqualTo(interestId);
  }

  @Test
  @DisplayName("존재하지 않는 사용자가 구독을 취소하면 예외가 발생한다")
  void unsubscribe_ThrowsException_WhenUserNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when
    ThrowingCallable action = () -> subscriptionService.unsubscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(subscriptionRepository, never()).deleteByUserIdAndInterestId(any(), any());
    verify(interestRepository, never()).decrementSubscriberCount(any());
  }

  @Test
  @DisplayName("삭제된 사용자가 구독을 취소하면 예외가 발생한다")
  void unsubscribe_ThrowsException_WhenUserDeleted() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User deletedUser = new User("deleted@example.com", "tester", "password");
    deletedUser.delete();

    given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

    // when
    ThrowingCallable action = () -> subscriptionService.unsubscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(subscriptionRepository, never()).deleteByUserIdAndInterestId(any(), any());
    verify(interestRepository, never()).decrementSubscriberCount(any());
  }

  @Test
  @DisplayName("구독하지 않은 관심사를 취소하면 예외가 발생한다")
  void unsubscribe_ThrowsException_WhenSubscriptionNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId)).willReturn(0);

    // when
    ThrowingCallable action = () -> subscriptionService.unsubscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.SUBSCRIPTION_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId, "userId", userId));
        });

    verify(interestRepository, never()).decrementSubscriberCount(any());
  }

  @Test
  @DisplayName("구독자 수 감소 결과가 0이면 예외가 발생한다")
  void unsubscribe_ThrowsException_WhenSubscriberCountDecrementFails() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    User user = new User("user@example.com", "tester", "password");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId)).willReturn(1);
    given(interestRepository.decrementSubscriberCount(interestId)).willReturn(0);

    // when
    ThrowingCallable action = () -> subscriptionService.unsubscribe(interestId, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId));
        });
  }

  private Interest interest(UUID id, String name) {
    Interest interest = new Interest(name);
    ReflectionTestUtils.setField(interest, "id", id);
    return interest;
  }

  private Keyword keyword(UUID id, String name) {
    Keyword keyword = new Keyword(name);
    ReflectionTestUtils.setField(keyword, "id", id);
    return keyword;
  }

  private Subscription subscription(UUID id, User user, Interest interest, Instant createdAt) {
    Subscription subscription = new Subscription(user, interest);
    ReflectionTestUtils.setField(subscription, "id", id);
    ReflectionTestUtils.setField(subscription, "createdAt", createdAt);
    return subscription;
  }
}
