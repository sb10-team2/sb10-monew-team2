package com.springboot.monew.users.repository;

import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  Optional<User> findByEmail(String email);

  // 논리 삭제된 user들 중에서 현재 시간에서 24시간을 뺀 시간인, cutoff보다 더 과거에 deletedAt이 찍힌 user들을 조회
  @Query("""
          select  u
          from User u
          where u.deletedAt is not null
            and u.deletedAt < :cutoff
      """)
  List<User> findUsersDeletedBefore(@Param("cutoff") Instant cutoff);

}
