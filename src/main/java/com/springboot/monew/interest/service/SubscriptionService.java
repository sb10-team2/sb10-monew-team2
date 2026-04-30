package com.springboot.monew.interest.service;

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
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserActivityOutboxService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;
  private final InterestRepository interestRepository;
  private final InterestKeywordRepository interestKeywordRepository;
  private final UserRepository userRepository;
  private final SubscriptionDtoMapper subscriptionDtoMapper;
  private final UserActivityOutboxService userActivityOutboxService;

  @Transactional
  public SubscriptionDto subscribe(UUID interestId, UUID userId) {
    // 요청 유저가 존재하는지 확인하고 삭제되지 않은 활성 사용자인지 검증
    User user = validateActiveUser(userId);
    // 존재하는 관심사인지 검증
    Interest interest = interestRepository.findById(interestId).orElseThrow(
        () -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
            Map.of("interestId", interestId)));

    // 이미 해당 관심사를 구독 중인지 확인
    if (subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)) {
      throw new InterestException(InterestErrorCode.SUBSCRIPTION_ALREADY_EXISTS,
          Map.of("interestId", interestId, "userId", userId));
    }

    // 구독 엔티티 저장
    // 동시성 상황에서 중복 저장이 발생할 수 있으므로 saveAndFlush 내부에서 처리
    Subscription subscription = saveSubscriptionSafely(user, interest, userId, interestId);
    // 관심사의 구독자 수를 DB에서 원자적으로 1 증가
    incrementSubscriberCount(interestId);

    // 해당 관심사에 연결된 키워드 목록 조회
    List<String> keywords = interestKeywordRepository.findAllByInterestWithKeyword(interest)
        .stream()
        .map(InterestKeyword::getKeyword)
        .map(Keyword::getName)
        .toList();

    // 증가가 반영된 최신 구독자 수 조회
    long subscriberCount = getSubscriberCount(interestId);

    // 관심사 구독 후 사용자 활동 반영을 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveInterestSubscribed(subscription, keywords);

    log.info("관심사 구독 완료 - interestId: {}, userId: {}", interestId, userId);
    return subscriptionDtoMapper.toSubscriptionDto(subscription, keywords, subscriberCount);
  }

  @Transactional
  public void unsubscribe(UUID interestId, UUID userId) {
    // 요청 유저가 존재하는지 확인하고 삭제되지 않은 활성 사용자인지 검증
    validateActiveUser(userId);

    // 해당 관심사를 실제로 구독 중인지 확인하고 구독 삭제
    deleteSubscription(interestId, userId);
    // 관심사의 구독자 수를 DB에서 원자적으로 1 감소
    decrementSubscriberCount(interestId);

    // 관심사 구독 취소 후 사용자 활동 반영을 위한 Outbox 이벤트를 저장한다.
    userActivityOutboxService.saveInterestUnsubscribed(userId, interestId);

    log.info("관심사 구독 취소 완료 - interestId: {}, userId: {}", interestId, userId);
  }

  private Subscription saveSubscriptionSafely(User user, Interest interest, UUID userId,
      UUID interestId) {
    try {
      // 구독 엔티티 저장 후 즉시 flush
      return subscriptionRepository.saveAndFlush(new Subscription(user, interest));
    } catch (DataIntegrityViolationException e) {
      log.debug("관심사 구독 실패 - interestId: {}, userId: {}", interestId, userId, e);
      // 동시 요청 등으로 중복 구독이 발생한 경우 중복 구독 예외로 변환
      if (subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)) {
        throw new InterestException(InterestErrorCode.SUBSCRIPTION_ALREADY_EXISTS,
            Map.of("interestId", interestId, "userId", userId));
      }

      // 존재하는 관심사인지 검증
      if (!interestRepository.existsById(interestId)) {
        throw new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
            Map.of("interestId", interestId));
      }

      // 요청 유저가 존재하는지 확인하고 삭제되지 않은 활성 사용자인지 검증
      validateActiveUser(userId);

      // 그 외 오류는 그대로 반환
      throw e;
    }
  }

  private void deleteSubscription(UUID interestId, UUID userId) {
    // 구독 row를 삭제하고 실제 삭제된 행 수를 확인
    int deletedRowCount = subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId);

    // 정확히 한 개의 구독만 삭제되어야 하므로, 삭제된 행 수가 1이 아니면 예외 발생
    if (deletedRowCount != 1) {
      throw new InterestException(InterestErrorCode.SUBSCRIPTION_NOT_FOUND,
          Map.of("interestId", interestId, "userId", userId));
    }
  }

  private void incrementSubscriberCount(UUID interestId) {
    // 구독자 수를 DB update 쿼리로 증가시키고 영향 받은 행 수를 확인
    int updatedRowCount = interestRepository.incrementSubscriberCount(interestId);

    // 정확히 한 개의 관심사만 수정되어야 하므로, 수정된 행 수가 1이 아니면 예외 발생
    if (updatedRowCount != 1) {
      throw new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
          Map.of("interestId", interestId));
    }
  }

  private void decrementSubscriberCount(UUID interestId) {
    // subscriberCount를 DB update 쿼리로 감소시키고 영향받은 행 수를 확인
    int updatedRowCount = interestRepository.decrementSubscriberCount(interestId);

    // 정확히 한 개의 관심사만 수정되어야 하므로, 수정된 행 수가 1이 아니면 예외 발생
    if (updatedRowCount != 1) {
      throw new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
          Map.of("interestId", interestId));
    }
  }

  private long getSubscriberCount(UUID interestId) {
    // DB에 반영된 최신 구독자 수 조회
    Long subscriberCount = interestRepository.findSubscriberCountById(interestId);

    // 구독자 수가 null이라면(해당 관심사가 없다면) 예외 발생
    if (subscriberCount == null) {
      throw new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
          Map.of("interestId", interestId));
    }

    // 조회된 최신 구독자 수 반환
    return subscriberCount;
  }

  private User validateActiveUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND,
            Map.of("userId", userId)));

    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }

    return user;
  }
}
