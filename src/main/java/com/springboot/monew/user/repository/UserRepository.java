package com.springboot.monew.user.repository;

import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.qdsl.UserQDSLRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID>, UserQDSLRepository {

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  Optional<User> findByEmail(String email);
}
