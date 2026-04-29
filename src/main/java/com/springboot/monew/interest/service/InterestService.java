package com.springboot.monew.interest.service;

import com.springboot.monew.interest.dto.request.InterestPageRequest;
import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.CursorPageResponseInterestDto;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.InterestOrderBy;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.exception.InterestErrorCode;
import com.springboot.monew.interest.exception.InterestException;
import com.springboot.monew.interest.mapper.InterestDtoMapper;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.interest.repository.SubscriptionRepository;
import com.springboot.monew.interest.util.StringSimilarityUtil;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.event.interest.InterestUpdatedEvent;
import com.springboot.monew.user.exception.UserErrorCode;
import com.springboot.monew.user.exception.UserException;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {

  // 유사도 임계값 (80%)
  private static final double NAME_SIMILARITY_THRESHOLD = 0.8;

  private final InterestRepository interestRepository;
  private final KeywordRepository keywordRepository;
  private final InterestKeywordRepository interestKeywordRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;
  private final InterestDtoMapper interestDtoMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public CursorPageResponseInterestDto list(InterestPageRequest request, UUID userId) {
    // 요청 유저가 존재하는지 확인하고 삭제되지 않은 활성 사용자인지 검증
    validateActiveUser(userId);

    // 요청 조건에 맞는 관심사 목록을 limit+1 개수만큼 조회
    List<Interest> interests = new ArrayList<>(interestRepository.findInterests(request));
    // 조회된 개수가 요청 limit을 초과하면 다음 페이지가 존재한다고 판단
    boolean hasNext = interests.size() > request.limit();

    // 다음 페이지가 존재하는 경우 반환 데이터는 limit 개수만큼 잘라내기
    if (hasNext) {
      interests = new ArrayList<>(interests.subList(0, request.limit()));
    }

    // 조회된 관심사 목록에서 각 관심사의 ID 목록을 추출
    List<UUID> interestIds = interests.stream()
        .map(Interest::getId)
        .toList();

    // 관심사 ID 목록을 기준으로 각 관심사에 연결된 키워드 목록을 조회하여 매핑
    Map<UUID, List<String>> keywordsByInterestId = getKeywordsByInterestId(interestIds);
    // 현재 유저가 구독 중인 관심사 ID 목록을 조회
    Set<UUID> subscribedInterestIds = getSubscribedInterestIds(userId, interestIds);

    // 관심사 엔티티를 dto로 변환하면서 키워드 목록과 구독 여부를 주입
    List<InterestDto> content = interests.stream()
        .map(interest -> interestDtoMapper.toInterestDto(
            interest,
            keywordsByInterestId.getOrDefault(interest.getId(), List.of()),
            subscribedInterestIds.contains(interest.getId())))
        .toList();

    // 마지막 관심사를 기준으로 다음 커서를 생성하기 위해 마지막 요소 추출
    Interest lastInterest = interests.isEmpty() ? null : interests.get(interests.size() - 1);
    // 다음 페이지가 존재하면 마지막 관심사를 기준으로 다음 커서를 생성하고 아니면 null로 설정
    String nextCursor =
        hasNext && lastInterest != null ? getNextCursor(request, lastInterest) : null;
    // 다음 페이지가 존재하면 마지막 관심사의 생성 시간을 after 값으로 설정하고 아니면 null로 설정
    Instant nextAfter = hasNext && lastInterest != null ? lastInterest.getCreatedAt() : null;
    // 전체 관심사 개수를 조회하여 페이지네이션 메타데이터로 사용
    long totalElements = interestRepository.countInterests(request.keyword());

    log.info("관심사 목록 조회 완료 - userId: {}, content.size: {}, hasNext: {}, totalElements: {}",
        userId, content.size(), hasNext, totalElements);
    // 조회 결과와 커서 정보, 전체 개수 등을 포함한 커서 기반 관심사 페이지 응답 dto 생성 및 반환
    return new CursorPageResponseInterestDto(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        totalElements,
        hasNext
    );
  }

  @Transactional
  public InterestDto create(InterestRegisterRequest request) {
    // 관심사 이름과 키워드 목록 추출
    String interestName = request.name();
    List<String> keywordNames = request.keywords();

    // 관심사 이름이 이미 존재하는지 검증
    validateDuplicateInterestName(interestName);
    // 80% 이상 유사한 관심사 이름이 존재하는지 검증
    validateSimilarInterestName(interestName);
    // 입력 받은 키워드 중 중복이 있는지 검증
    validateDuplicateKeywords(keywordNames);

    // 관심사 저장
    Interest interest = interestRepository.save(new Interest(interestName));

    // 요청 받은 키워드 목록 순회
    for (String keywordName : keywordNames) {
      Keyword keyword = getOrCreateKeyword(keywordName);
      interestKeywordRepository.save(new InterestKeyword(interest, keyword));
    }

    log.info("관심사 등록 완료 - interestId: {}, interestName: {}, keywordNames: {}", interest.getId(),
        interest.getName(), keywordNames);
    return interestDtoMapper.toInterestDto(interest, keywordNames, false);
  }

  @Transactional
  public InterestDto update(UUID interestId, InterestUpdateRequest request) {
    // 관심사 조회, 존재하지 않는 관심사라면 예외 발생
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
            Map.of("interestId", interestId)));

    // 키워드 목록 추출
    List<String> keywordNames = request.keywords();
    // 입력 받은 키워드 중 중복이 있는지 검증
    validateDuplicateKeywords(keywordNames);

    // 해당 관심사의 연결 목록 조회
    List<InterestKeyword> existingInterestKeywords =
        interestKeywordRepository.findAllByInterestWithKeyword(interest);

    // 연결 내용을 키워드 이름 기준으로 빠르게 조회하기 위해 (키워드 이름, 관심사-키워드 연결 엔티티)) 형태로 맵 생성
    Map<String, InterestKeyword> existingInterestKeywordMap = new LinkedHashMap<>();
    for (InterestKeyword interestKeyword : existingInterestKeywords) {
      existingInterestKeywordMap.put(interestKeyword.getKeyword().getName(), interestKeyword);
    }

    // 요청 키워드 목록을 Set으로 생성
    Set<String> requestedKeywordNames = new HashSet<>(keywordNames);

    // 요청 목록에 없는 키워드만 선별하여 삭제 대상으로 생성
    List<InterestKeyword> interestKeywordsToDelete = existingInterestKeywords.stream()
        .filter(interestKeyword -> !requestedKeywordNames.contains(
            interestKeyword.getKeyword().getName()))
        .toList();

    // 삭제 대상에 포함된 키워드 중 고아 객체 수집
    List<Keyword> orphanCandidateKeywords = interestKeywordsToDelete.stream()
        .map(InterestKeyword::getKeyword)
        .toList();

    // 삭제 대상이 있으면 삭제
    if (!interestKeywordsToDelete.isEmpty()) {
      interestKeywordRepository.deleteAll(interestKeywordsToDelete);
    }

    // 요청 목록을 순회하며 기존 연결이 없는 새로운 키워드만 새롭게 생성
    for (String keywordName : keywordNames) {
      if (existingInterestKeywordMap.containsKey(keywordName)) {
        continue;
      }

      Keyword keyword = getOrCreateKeyword(keywordName);
      interestKeywordRepository.save(new InterestKeyword(interest, keyword));
    }

    // 더 이상 연결된 관심사가 없는 키워드는 삭제
    deleteOrphanKeywords(orphanCandidateKeywords);

    // 관심사 키워드 수정 후, 해당 관심사를 구독 중인 사용자들의 활동 내역 구독 정보를 최신 키워드로 갱신하기 위해 이벤트를 발행한다.
    eventPublisher.publishEvent(
        new InterestUpdatedEvent(interest.getId(), keywordNames)
    );

    log.info("관심사 수정 완료 - interestId: {}, keywordNames: {}", interest.getId(), keywordNames);
    return interestDtoMapper.toInterestDto(interest, keywordNames, false);
  }

  @Transactional
  public void delete(UUID interestId) {
    // 관심사 조회, 존재하지 않는 관심사라면 예외 발생
    Interest interest = interestRepository.findById(interestId)
        .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND,
            Map.of("interestId", interestId)));

    // 해당 관심사의 연결 목록 조회
    List<InterestKeyword> existingInterestKeywords = interestKeywordRepository.findAllByInterestWithKeyword(
        interest);
    // 해당 관심사의 기존 키워드 목록 추출
    List<Keyword> oldKeywords = existingInterestKeywords.stream()
        .map(InterestKeyword::getKeyword)
        .toList();

    // 조회한 관심사-키워드 연결 삭제
    interestKeywordRepository.deleteAll(existingInterestKeywords);
    // 관심사 삭제
    interestRepository.delete(interest);

    // 관심사와 연결되어 있던 키워드 목록을 순회하며 더 이상 연결된 관심사가 없다면 삭제
    deleteOrphanKeywords(oldKeywords);

    log.info("관심사 삭제 완료 - interestId={}, interestName={}", interest.getId(), interest.getName());
  }

  private Keyword getOrCreateKeyword(String keywordName) {
    return keywordRepository.findByName(keywordName)
        .orElseGet(() -> saveKeywordSafely(keywordName));
  }

  private Keyword saveKeywordSafely(String keywordName) {
    try {
      return keywordRepository.saveAndFlush(new Keyword(keywordName));
    } catch (DataIntegrityViolationException e) {
      log.debug("키워드 동시 생성 충돌 발생 - 기존 키워드 재조회: {}", keywordName);
      return keywordRepository.findByName(keywordName)
          .orElseThrow(() -> e);
    }
  }

  private void validateDuplicateInterestName(String interestName) {
    // 관심사 이름이 이미 존재하면 예외 발생
    if (interestRepository.existsByName(interestName)) {
      throw new InterestException(InterestErrorCode.INTEREST_NAME_DUPLICATION,
          Map.of("name", interestName));
    }
  }

  private void validateSimilarInterestName(String interestName) {
    // 관심사 이름 전체 조회
    List<String> existingNames = interestRepository.findAllNames();

    // 관심사 이름 순회
    for (String existingName : existingNames) {
      // 관심사 이름 유사도 체크
      if (StringSimilarityUtil.isSimilarEnough(interestName, existingName,
          NAME_SIMILARITY_THRESHOLD)) {
        throw new InterestException(InterestErrorCode.INTEREST_NAME_DUPLICATION,
            Map.of("name", interestName));
      }
    }
  }

  private void validateDuplicateKeywords(List<String> keywords) {
    // 키워드 목록 중복 제거 후 개수 파악
    long distinctCount = keywords.stream()
        .distinct()
        .count();

    // 중복으로 제거된 키워드가 있다면 예외 발생
    if (distinctCount != keywords.size()) {
      throw new InterestException(InterestErrorCode.DUPLICATE_KEYWORDS, Map.of());
    }
  }

  private void deleteOrphanKeywords(List<Keyword> keywords) {
    if (keywords.isEmpty()) {
      return;
    }

    // 키워드 객체 리스트에서 id를 추출하여 id 리스트로 변환
    List<UUID> keywordIds = keywords.stream()
        .map(Keyword::getId)
        .toList();

    keywordRepository.deleteOrphanKeywordsByIds(keywordIds);
  }

  private Map<UUID, List<String>> getKeywordsByInterestId(List<UUID> interestIds) {
    if (interestIds.isEmpty()) {
      return Map.of();
    }

    // 각 관심사 ID를 키로 가지는 결과 맵 초기화
    Map<UUID, List<String>> keywordsByInterestId = new LinkedHashMap<>();
    for (UUID interestId : interestIds) {
      keywordsByInterestId.put(interestId, new ArrayList<>());
    }

    // 관심사 ID 목록에 해당하는 관심사-키워드 연결 목록을 조회
    List<InterestKeyword> interestKeywords =
        interestKeywordRepository.findAllByInterestIdInWithKeyword(interestIds);
    for (InterestKeyword interestKeyword : interestKeywords) {
      // 관심사 ID를 기준으로 해당 키워드 이름을 결과 맵에 추가
      UUID interestId = interestKeyword.getInterest().getId();
      keywordsByInterestId
          .computeIfAbsent(interestId, id -> new ArrayList<>())
          .add(interestKeyword.getKeyword().getName());
    }

    // 관심사별 키워드 이름 목록이 매핑된 결과를 반환
    return keywordsByInterestId;
  }

  private Set<UUID> getSubscribedInterestIds(UUID userId, List<UUID> interestIds) {
    if (interestIds.isEmpty()) {
      return Set.of();
    }

    // 유저가 구독한 관심사 ID 목록을 조회하여 Set으로 변환해 반환
    return new HashSet<>(
        subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(userId, interestIds));
  }

  private String getNextCursor(InterestPageRequest request, Interest interest) {
    // 요청한 정렬 기준에 맞는 커서 값을 관심사 엔티티에서 추출하여 반환
    String cursor = request.orderBy().getCursor(interest);

    // 정렬값이 구독자 수일 경우 구독자 수가 같은 데이터의 누락을 막기 위해 생성 시각을 커서에 포함
    // 현재 프론트엔트 코드에선 after를 전달하지 않음
    if (request.orderBy() == InterestOrderBy.subscriberCount) {
      return cursor + "|" + interest.getCreatedAt();
    }

    return cursor;
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
