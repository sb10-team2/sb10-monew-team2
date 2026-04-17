package com.springboot.monew.interest.service;

import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.mapper.InterestDtoMapper;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.interest.util.StringSimilarityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestService {

    private static final double NAME_SIMILARITY_THRESHOLD = 0.8;

    private final InterestRepository interestRepository;
    private final KeywordRepository keywordRepository;
    private final InterestKeywordRepository interestKeywordRepository;
    private final InterestDtoMapper interestDtoMapper;

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

        return interestDtoMapper.toInterestDto(interest, keywordNames, false);
    }

    private void validateDuplicateInterestName(String interestName) {
        if (interestRepository.existsByName(interestName)) {
            // 예외 개선 필요 - "이미 존재하는 관심사 이름입니다."
            throw new RuntimeException();
        }
    }

    private void validateSimilarInterestName(String interestName) {
        List<String> existingNames = interestRepository.findAllNames();

        for (String existingName : existingNames) {
            if (StringSimilarityUtil.isSimilarEnough(interestName, existingName, NAME_SIMILARITY_THRESHOLD)) {
                // 예외 개선 필요 - "유사도 80% 이상의 관심사 이름은 등록할 수 없습니다."
                throw new RuntimeException();
            }
        }
    }

    private void validateDuplicateKeywords(List<String> keywords) {
        long distinctCount = keywords.stream()
                .distinct()
                .count();

        if (distinctCount != keywords.size()) {
            // 예외 개선 필요 - "키워드는 중복될 수 없습니다."
            throw new RuntimeException();
        }
    }
}
