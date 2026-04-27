package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.dto.response.InterestKeywordInfo;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
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
          ORDER BY ik.createdAt ASC
      """)
  List<InterestKeyword> findAllByInterestWithKeyword(@Param("interest") Interest interest);

  @Query("""
          SELECT ik
          FROM InterestKeyword ik
          JOIN FETCH ik.interest
          JOIN FETCH ik.keyword
          WHERE ik.interest.id IN :interestIds
          ORDER BY ik.interest.id ASC, ik.createdAt ASC
      """)
  List<InterestKeyword> findAllByInterestIdInWithKeyword(
      @Param("interestIds") List<UUID> interestIds);

  @Query("""
          SELECT DISTINCT ik.keyword.id
          FROM InterestKeyword ik
          WHERE ik.keyword.id IN :keywordIds
      """)
  List<UUID> findReferencedKeywordIds(@Param("keywordIds") List<UUID> keywordIds);

  @Query("""
          SELECT
              ik.interest.id AS interestId,
              ik.keyword.name AS keywordName
          FROM InterestKeyword ik
      """)
  List<InterestKeywordInfo> findAllInterestKeywordInfos();
}
