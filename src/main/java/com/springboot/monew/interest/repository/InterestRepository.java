package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.repository.qdsl.InterestQDSLRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID>, InterestQDSLRepository {

  boolean existsByName(String name);

  @Query("SELECT i.name FROM Interest i")
  List<String> findAllNames();

  @Modifying
  @Query("""
      UPDATE Interest i
      SET i.subscriberCount = i.subscriberCount + 1
      WHERE i.id = :interestId
      """)
  int incrementSubscriberCount(@Param("interestId") UUID interestId);

  @Modifying
  @Query("""
      UPDATE Interest i
      SET i.subscriberCount =
          CASE
            WHEN i.subscriberCount > 0 THEN i.subscriberCount - 1
            ELSE 0
          END
      WHERE i.id = :interestId
      """)
  int decrementSubscriberCount(@Param("interestId") UUID interestId);

  @Query("""
      SELECT i.subscriberCount
      FROM Interest i
      WHERE i.id = :interestId
      """)
  Long findSubscriberCountById(@Param("interestId") UUID interestId);
}
