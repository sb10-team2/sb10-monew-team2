package com.springboot.monew.user.repository;

import com.springboot.monew.user.document.UserActivityDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID> {

  // subscriptions에 해당 interestId를 가진 구독 항목이 포함된 사용자 활동 문서를 모두 조회한다.
  List<UserActivityDocument> findAllBySubscriptionsInterestId(UUID interestId);
}
