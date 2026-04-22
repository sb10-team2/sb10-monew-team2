package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.dto.response.InterestKeywordInfo;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

  @Query("""
          SELECT ik
          FROM InterestKeyword ik
          JOIN FETCH ik.keyword
          WHERE ik.interest = :interest
      """)
  List<InterestKeyword> findAllByInterestWithKeyword(@Param("interest") Interest interest);

  @Query("""
          SELECT DISTINCT ik.keyword.id
          FROM InterestKeyword ik
          WHERE ik.keyword.id IN :keywordIds
      """)
  List<UUID> findReferencedKeywordIds(@Param("keywordIds") List<UUID> keywordIds);

    //관심사의 키워드 목록
    @Query("""
    select
        ik.interest.id as interestId,
        ik.keyword.name as keywordName
    from InterestKeyword ik
""")
    List<InterestKeywordInfo> findAllInterestKeywordInfos();
}
