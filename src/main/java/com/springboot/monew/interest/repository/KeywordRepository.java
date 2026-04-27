package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Keyword;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KeywordRepository extends JpaRepository<Keyword, UUID> {

  Optional<Keyword> findByName(String name);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
          DELETE FROM Keyword k
          WHERE k.id IN :keywordIds
            AND NOT EXISTS (
                SELECT 1
                FROM InterestKeyword ik
                WHERE ik.keyword = k
            )
      """)
  int deleteOrphanKeywordsByIds(@Param("keywordIds") Collection<UUID> keywordIds);
}
