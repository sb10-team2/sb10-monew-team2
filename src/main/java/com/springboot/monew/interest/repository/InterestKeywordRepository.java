package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.dto.response.InterestKeywordInfo;
import com.springboot.monew.interest.entity.InterestKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

    //관심사의 키워드 목록
    @Query("""
    select
        ik.interest.id as interestId,
        ik.keyword.name as keywordName
    from InterestKeyword ik
""")
    List<InterestKeywordInfo> findAllInterestKeywordInfos();
}
