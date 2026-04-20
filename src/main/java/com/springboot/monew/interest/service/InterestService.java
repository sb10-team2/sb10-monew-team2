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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            // 새로운 키워드라면 저장
            Keyword keyword = keywordRepository.findByName(keywordName)
                    .orElseGet(() -> keywordRepository.save(new Keyword(keywordName)));

            // 관심사-키워드 연결 저장
            interestKeywordRepository.save(new InterestKeyword(interest, keyword));
        }

        log.info("관심사 등록 완료 - interestId: {}, interestName: {}, keywordNames: {}", interest.getId(), interest.getName(), keywordNames);
        return interestDtoMapper.toInterestDto(interest, keywordNames, false);
    }

    @Transactional
    public InterestDto update(UUID interestId, InterestUpdateRequest request) {
        // 관심사 조회, 존재하지 않는 관심사라면 예외 발생
        Interest interest = interestRepository.findById(interestId)
                .orElseThrow(() -> new InterestException(InterestErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId)));

        // 키워드 목록 추출
        List<String> keywordNames = request.keywords();
        // 입력 받은 키워드 중 중복이 있는지 검증
        validateDuplicateKeywords(keywordNames);

        // 해당 관심사의 연결 목록 조회
        List<InterestKeyword> existingInterestKeywords = interestKeywordRepository.findAllByInterest(interest);
        // 해당 관심사의 기존 키워드 목록 추출
        List<Keyword> oldKeywords = existingInterestKeywords.stream()
                .map(InterestKeyword::getKeyword)
                .toList();

        // 조회한 관심사-키워드 연결 삭제
        interestKeywordRepository.deleteAll(existingInterestKeywords);

        // 요청 받은 키워드 목록 순회
        for (String keywordName : keywordNames) {
            // 새로운 키워드라면 저장
            Keyword keyword = keywordRepository.findByName(keywordName)
                    .orElseGet(() -> keywordRepository.save(new Keyword(keywordName)));

            // 관심사-키워드 연결 저장
            interestKeywordRepository.save(new InterestKeyword(interest, keyword));
        }

        // 이전 키워드 목록을 순회하며 더 이상 연결된 관심사가 없다면 삭제
        deleteOrphanKeywords(oldKeywords);

        log.info("관심사 수정 완료 - interestId: {}, keywordNames: {}", interest.getId(), keywordNames);
        return interestDtoMapper.toInterestDto(interest, keywordNames, false);
    }

    private void validateDuplicateInterestName(String interestName) {
        // 관심사 이름이 이미 존재하면 예외 발생
        if (interestRepository.existsByName(interestName)) {
            throw new InterestException(InterestErrorCode.INTEREST_NAME_DUPLICATION, Map.of("name", interestName));
        }
    }

    private void validateSimilarInterestName(String interestName) {
        // 관심사 이름 전체 조회
        List<String> existingNames = interestRepository.findAllNames();

        // 관심사 이름 순회
        for (String existingName : existingNames) {
            // 관심사 이름 유사도 체크
            if (StringSimilarityUtil.isSimilarEnough(interestName, existingName, NAME_SIMILARITY_THRESHOLD)) {
                throw new InterestException(InterestErrorCode.INTEREST_NAME_DUPLICATION, Map.of("name", interestName));
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
        for (Keyword keyword : keywords) {
            // 해당 키워드와 연결된 관심사가 존재하지 않는다면
            if (!interestKeywordRepository.existsByKeyword(keyword)) {
                // 키워드 삭제
                keywordRepository.delete(keyword);
            }
        }
    }

}
