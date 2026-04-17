package com.springboot.monew.interest.service;

import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestService {

    private static final double NAME_SIMILARITY_THRESHOLD = 0.8;

    private final InterestRepository interestRepository;
    private final KeywordRepository keywordRepository;
    private final InterestKeywordRepository interestKeywordRepository;
    private final InterestDtoMapper interestDtoMapper;

    @Transactional
    public InterestDto create(InterestRegisterRequest request) {
        String interestName = request.name();
        List<String> keywordNames = request.keywords();

        validateDuplicateInterestName(interestName);
        validateSimilarInterestName(interestName);
        validateDuplicateKeywords(keywordNames);

        Interest interest = interestRepository.save(new Interest(interestName));

        for (String keywordName : keywordNames) {
            Keyword keyword = keywordRepository.findByName(keywordName)
                    .orElseGet(() -> keywordRepository.save(new Keyword(keywordName)));

            interestKeywordRepository.save(new InterestKeyword(interest, keyword));
        }

        log.info("관심사 등록 완료 - interestId: {}, interestName: {}, keywordNames: {}", interest.getId(), interest.getName(), keywordNames);
        return interestDtoMapper.toInterestDto(interest, keywordNames, false);
    }

    private void validateDuplicateInterestName(String interestName) {
        if (interestRepository.existsByName(interestName)) {
            throw new InterestException(InterestErrorCode.INTEREST_NAME_ALREADY_EXISTS, Map.of("name", interestName));
        }
    }

    private void validateSimilarInterestName(String interestName) {
        List<String> existingNames = interestRepository.findAllNames();

        for (String existingName : existingNames) {
            if (StringSimilarityUtil.isSimilarEnough(interestName, existingName, NAME_SIMILARITY_THRESHOLD)) {
                throw new InterestException(InterestErrorCode.INTEREST_NAME_SIMILARITY_CONFLICT, Map.of("name", interestName));
            }
        }
    }

    private void validateDuplicateKeywords(List<String> keywords) {
        long distinctCount = keywords.stream()
                .distinct()
                .count();

        if (distinctCount != keywords.size()) {
            throw new InterestException(InterestErrorCode.DUPLICATE_KEYWORDS, Map.of());
        }
    }
}
