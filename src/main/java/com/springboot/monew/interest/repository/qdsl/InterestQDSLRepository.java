package com.springboot.monew.interest.repository.qdsl;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.dto.request.InterestPageRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterestQDSLRepository {

  Optional<Interest> findByIdWithArticleCount(UUID id);

  List<Interest> findInterests(InterestPageRequest request);

  long countInterests(String keyword);
}
