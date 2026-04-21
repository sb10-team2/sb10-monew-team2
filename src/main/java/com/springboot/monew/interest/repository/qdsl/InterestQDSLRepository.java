package com.springboot.monew.interest.repository.qdsl;

import com.springboot.monew.interest.entity.Interest;
import java.util.Optional;
import java.util.UUID;

public interface InterestQDSLRepository {

  Optional<Interest> findByIdWithArticleCount(UUID id);
}
