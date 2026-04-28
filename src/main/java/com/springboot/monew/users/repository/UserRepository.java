package com.springboot.monew.users.repository;

import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.repository.qdsl.UserQDSLRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID>, UserQDSLRepository {

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);

  Optional<User> findByEmail(String email);
}
