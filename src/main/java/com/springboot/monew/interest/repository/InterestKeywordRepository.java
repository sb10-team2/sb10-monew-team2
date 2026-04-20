package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

  List<InterestKeyword> findAllByInterest(Interest interest);

  boolean existsByKeyword(Keyword keyword);
}
