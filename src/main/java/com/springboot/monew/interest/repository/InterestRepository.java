package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID> {
}
