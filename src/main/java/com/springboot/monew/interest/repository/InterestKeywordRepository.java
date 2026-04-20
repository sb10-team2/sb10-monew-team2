package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

  List<InterestKeyword> findAllByInterest(Interest interest);

  boolean existsByKeyword(Keyword keyword);
}
