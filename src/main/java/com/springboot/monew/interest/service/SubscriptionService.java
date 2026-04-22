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
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.repository.UserRepository;
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
    // 관심사의 구독자 수 1 증가
    interest.increaseSubscriberCount();

    // 해당 관심사에 연결된 키워드 목록 조회
    List<String> keywords = interestKeywordRepository.findAllByInterestWithKeyword(interest)
        .stream()
        .map(InterestKeyword::getKeyword)
        .map(Keyword::getName)
        .toList();

    log.info("관심사 구독 완료 - interestId: {}, userId: {}", interestId, userId);
    return subscriptionDtoMapper.toSubscriptionDto(subscription, keywords);
  }

  private Subscription saveSubscriptionSafely(User user, Interest interest, UUID userId,
      UUID interestId) {
    try {
      // 구독 엔티티 저장 후 즉시 flush
      return subscriptionRepository.saveAndFlush(new Subscription(user, interest));
    } catch (DataIntegrityViolationException e) {
      log.debug("관심사 구독 동시성 충돌 발생 - interestId: {}, userId: {}", interestId, userId);
      throw new InterestException(InterestErrorCode.SUBSCRIPTION_ALREADY_EXISTS,
          Map.of("interestId", interestId, "userId", userId));
    }
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
