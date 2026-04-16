package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.InterestKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {
}
