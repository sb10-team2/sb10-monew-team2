package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.InterestKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

    //관심사의 키워드 목록
    @Query("select distinct ik.keyword.name from InterestKeyword ik")
    List<String> findAllKeywords();
}
