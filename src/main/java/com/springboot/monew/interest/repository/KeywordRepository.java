package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KeywordRepository extends JpaRepository<Keyword, UUID> {

    Optional<Keyword> findByName(String name);
}
