package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  @Query("""
          SELECT s.interest.id
          FROM Subscription s
          WHERE s.user.id = :userId
            AND s.interest.id IN :interestIds
      """)
  List<UUID> findInterestIdsByUserIdAndInterestIdIn(@Param("userId") UUID userId,
      @Param("interestIds") List<UUID> interestIds);
}
