package com.springboot.monew.interest.service;

import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.exception.InterestErrorCode;
import com.springboot.monew.interest.exception.InterestException;
import com.springboot.monew.interest.mapper.InterestDtoMapper;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.interest.util.StringSimilarityUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final InterestDtoMapper interestDtoMapper;

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
    List<InterestKeyword> existingInterestKeywords = interestKeywordRepository.findAllByInterestWithKeyword(
        interest);
    // 해당 관심사의 기존 키워드 목록 추출
    List<Keyword> oldKeywords = existingInterestKeywords.stream()
        .map(InterestKeyword::getKeyword)
        .toList();

    // 조회한 관심사-키워드 연결 삭제
    interestKeywordRepository.deleteAll(existingInterestKeywords);

    // 요청 받은 키워드 목록 순회
    for (String keywordName : keywordNames) {
      Keyword keyword = getOrCreateKeyword(keywordName);
      interestKeywordRepository.save(new InterestKeyword(interest, keyword));
    }

    // 이전 키워드 목록을 순회하며 더 이상 연결된 관심사가 없다면 삭제
    deleteOrphanKeywords(oldKeywords);

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
      log.debug("키워드 동시 생성 충돌 발생. 기존 키워드 재조회: {}", keywordName);
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

    // 아직 참조 중인 키워드가 있는지 한번에 조회
    Set<UUID> referencedKeywordIds = new HashSet<>(
        interestKeywordRepository.findReferencedKeywordIds(keywordIds)
    );

    // 더 이상 관심사 연결이 없는 키워드만 추출
    List<Keyword> orphanKeywords = keywords.stream()
        .filter(keyword -> !referencedKeywordIds.contains(keyword.getId()))
        .toList();

    // 더 이상 관심사 연결이 없는 키워드가 존재한다면 삭제
    if (!orphanKeywords.isEmpty()) {
      keywordRepository.deleteAll(orphanKeywords);
    }
  }

}
