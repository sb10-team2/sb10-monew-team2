package com.springboot.monew.interest.repository;

import com.springboot.monew.interest.entity.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

}
