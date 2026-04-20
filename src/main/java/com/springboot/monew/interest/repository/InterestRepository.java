package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Interest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

  boolean existsByName(String name);

  @Query("SELECT i.name FROM Interest i")
  List<String> findAllNames();
}
